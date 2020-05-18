package tla.backend.api;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import tla.backend.AbstractMockMvcTest;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.parts.EditDate;
import tla.backend.es.model.parts.EditorInfo;
import tla.backend.es.model.parts.LemmaWord;
import tla.backend.es.model.parts.Transcription;
import tla.backend.es.model.parts.Translations;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.service.LemmaService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class LemmaControllerTest extends AbstractMockMvcTest {

    @MockBean
    private LemmaRepo repo;

    private ObjectMapper mapper = new ObjectMapper();

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
        LemmaEntity l = mapper.readValue("{\"id\":\"ID\",\"sort_string\":\"1\"}", LemmaEntity.class);
        assertEquals("1", l.getSortKey(), "sort_string should be deserialized correctly");
    }

    @Test
    void mapLemmaEquals() throws Exception {
        LemmaEntity l1 = LemmaEntity.builder()
            .id("1")
            .eclass("BTSLemmaEntry")
            .editors(EditorInfo.builder().author("author").updated(EditDate.of(1854,10,31)).build())
            .translations(Translations.builder().de("übersetzung").build())
            .word(new LemmaWord("N35:G47", new Transcription("nfr", "nfr")))
            .build();
        String ser = new ObjectMapper().writeValueAsString(l1);
        LemmaEntity l2 = mapper.readValue(ser, LemmaEntity.class);
        assertAll("lemma instance created via lombok builder should be the same after being serialized by object mapper and deserialized by ES entity mapper",
            () -> assertEquals(l1, l2, "equals should return true"),
            () -> assertEquals(mapper.writeValueAsString(l1), mapper.writeValueAsString(l2), "ES entity mapper serializations should be equal"),
            () -> assertEquals(l1.hashCode(), l2.hashCode(), "hashCode should return equals"),
            () -> assertNotNull(l2.getWords().get(0).getTranscription(), "expect word transcription")
        );
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