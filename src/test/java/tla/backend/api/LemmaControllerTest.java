package tla.backend.api;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.test.web.servlet.MockMvc;

import tla.backend.App;
import tla.backend.Util;
import tla.backend.es.model.Lemma;
import tla.backend.es.model.Translations;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.es.model.EditorInfo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(classes = {App.class})
@AutoConfigureMockMvc
public class LemmaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LemmaRepo repo;

    @Autowired
    private EntityMapper mapper;

    @Test
    void nonNullFieldValidation() {
        assertThrows(NullPointerException.class,
            () -> {Lemma.builder().build();},
            "building lemma with null-ID should throw exception"
        );
    }

    @Test
    void mapLemma() throws Exception {
        assertNotNull(mapper, "elasticsearch jackson-based mapper should not be null");
        Lemma l = mapper.mapToObject("{\"id\":\"ID\",\"sort_string\":\"1\"}", Lemma.class);
        assertEquals("1", l.getSortKey(), "sort_string should be deserialized correctly");
    }

    @Test
    void mapLemmaEquals() throws Exception {
        Lemma l1 = Lemma.builder()
            .id("1")
            .eclass("BTSLemmaEntry")
            .editors(EditorInfo.builder().author("author").updated(Util.date("1854-10-31")).build())
            .translations(Translations.builder().de("übersetzung").build())
            .build();
        String ser = new ObjectMapper().writeValueAsString(l1);
        Lemma l2 = mapper.mapToObject(ser, Lemma.class);
        assertAll("lemma instance serialized by object mapper and deserialized by ES entity mapper should be same as source",
            () -> {assertEquals(l1, l2, "equals should return true");},
            () -> {assertEquals(l1.hashCode(), l2.hashCode(), "hashCode should return equals");}
        );
    }

    @Test
    void postLemma() throws Exception {
        when(repo.save(any()))
            .thenReturn(Lemma.builder().id("1").sortKey("A").build());
        Lemma l = repo.save(Lemma.builder().id("2").build());
        assertEquals("1", l.getId(), "whatever is being saved by mock up repo, it should always return a lemma with ID '1'");
        mockMvc.perform(
            post("/lemma/post")
                .contentType("application/json")
                .content("{\"id\":\"1\",\"sort_string\":\"A\"}")
        )
            .andExpect(
                status().isOk()
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
                    Lemma.builder()
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
            .andExpect(jsonPath("$.id").value("ID"))
            .andExpect(jsonPath("$.editors.author").value("author"))
            .andExpect(jsonPath("$.sortKey").value("1"))
            .andExpect(jsonPath("$.translations.de[0]").value("deutsch"))
            .andExpect(jsonPath("$.translations.en[0]").value("english"));
    }

}