package tla.backend.api;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.EntityMapper;

import tla.backend.AbstractMockMvcTest;
import tla.backend.Util;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.Translations;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.es.model.EditorInfo;
import tla.backend.service.LemmaService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class LemmaControllerTest extends AbstractMockMvcTest {

    @MockBean
    private LemmaRepo repo;

    @Autowired
    private EntityMapper mapper;

    @Autowired
    private LemmaService service;

    @Test
    void serviceInjected() {
        assertNotNull(service);
    }

    @Test
    void nonNullFieldValidation() {
        assertThrows(NullPointerException.class,
            () -> {LemmaEntity.builder().build();},
            "building lemma with null-ID should throw exception"
        );
    }

    @Test
    void mapLemma() throws Exception {
        assertNotNull(mapper, "elasticsearch jackson-based mapper should not be null");
        LemmaEntity l = mapper.mapToObject("{\"id\":\"ID\",\"sort_string\":\"1\"}", LemmaEntity.class);
        assertEquals("1", l.getSortKey(), "sort_string should be deserialized correctly");
    }

    @Test
    void mapLemmaEquals() throws Exception {
        LemmaEntity l1 = LemmaEntity.builder()
            .id("1")
            .eclass("BTSLemmaEntry")
            .editors(EditorInfo.builder().author("author").updated(Util.date("1854-10-31")).build())
            .translations(Translations.builder().de("übersetzung").build())
            .build();
        String ser = new ObjectMapper().writeValueAsString(l1);
        LemmaEntity l2 = mapper.mapToObject(ser, LemmaEntity.class);
        assertAll("lemma instance created via lombok builder should be the same after being serialized by object mapper and deserialized by ES entity mapper",
            () -> assertEquals(l1, l2, "equals should return true"),
            () -> assertEquals(mapper.mapToString(l1), mapper.mapToString(l2), "ES entity mapper serializations should be equal"),
            () -> assertEquals(l1.hashCode(), l2.hashCode(), "hashCode should return equals")
        );
    }

    @Test
    void postLemma() throws Exception {
        when(repo.save(any()))
            .thenReturn(
                LemmaEntity.builder()
                    .id("1")
                    .eclass("BTSLemmaEntry")
                    .sortKey("A")
                    .build()
                );
        LemmaEntity l = repo.save(
            LemmaEntity.builder()
                .id("2")
                .eclass("BTSLemmaEntry")
                .build()
            );
        assertEquals("1", l.getId(), "whatever is being saved by mock up repo, it should always return a lemma with ID '1'");
        mockMvc.perform(
            post("/lemma/post")
                .contentType("application/json")
                .content("{\"id\":\"1\",\"sort_string\":\"A\"}")
        )
            .andExpect(
                status().isCreated()
            )
            .andExpect(jsonPath("$.id").value("1"))
            .andExpect(jsonPath("$.sortKey").value("A"));
    }

    @Test
    void getInexistentLemma() throws Exception {
        mockMvc.perform(
            get("/lemma/get/10070")
                .contentType("application/json")
        )
            .andExpect(
                status().isNotFound()
            );
    }

    @Test
    void getExistingLemma() throws Exception {
        when(repo.findById(anyString()))
            .thenReturn(
                Optional.of(
                    LemmaEntity.builder()
                        .id("ID")
                        .eclass("BTSLemmaEntry")
                        .editors(
                            EditorInfo.builder()
                                .author("author")
                                .build()
                        )
                        .sortKey("1")
                        .translations(Translations.builder().de("deutsch").en("english").build())
                        .build()
                )
            );
        mockMvc.perform(
            get("/lemma/get/whatever")
                .contentType("application/json")
        )
            .andExpect(
                status().isOk()
            )
            .andExpect(jsonPath("$.doc.id").value("ID"))
            .andExpect(jsonPath("$.doc.editors.author").value("author"))
            .andExpect(jsonPath("$.doc.sortKey").value("1"))
            .andExpect(jsonPath("$.doc.translations.de[0]").value("deutsch"))
            .andExpect(jsonPath("$.doc.translations.en[0]").value("english"));
    }

}