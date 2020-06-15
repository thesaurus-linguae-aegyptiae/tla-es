package tla.backend.api;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import tla.backend.AbstractMockMvcTest;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.es.repo.TextRepo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


public class SentenceControllerTest extends AbstractMockMvcTest {

    @MockBean
    private SentenceRepo sentenceRepo;

    @MockBean
    private TextRepo textRepo;

    private TextEntity loadTextFromFile(String textId) throws Exception {
        return tla.domain.util.IO.loadFromFile(
            String.format("src/test/resources/sample/text/%s.json", textId),
            TextEntity.class
        );
    }

    private SentenceEntity loadSentenceFromFile(String sentenceId) throws Exception {
        return tla.domain.util.IO.loadFromFile(
            String.format("src/test/resources/sample/sentence/%s.json", sentenceId),
            SentenceEntity.class
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"CDWYGHBII5C37IBETSSI6RCIDQ"})
    void deserializeTextEntity(String textId) throws Exception {
        TextEntity t = loadTextFromFile(textId);
        assertAll("text deserialized",
            () -> assertNotNull(t, "not null"),
            () -> assertNotNull(t.getPaths(), "object paths instantiated")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"IBUBd3QvPWhrgk50h3u3Wv5PmdA"})
    void deserializeSentenceFromFile(String sentenceId) throws Exception {
        SentenceEntity s = loadSentenceFromFile(sentenceId);
        assertAll("check deserialized sentence entity",
            () -> assertNotNull(s, "not null"),
            () -> assertNotNull(s.getTokens(), "has tokens")
        );
    }

    @Test
    void getDetails() throws Exception {
        String sentenceId = "IBUBd3QvPWhrgk50h3u3Wv5PmdA";
        String textId = "CDWYGHBII5C37IBETSSI6RCIDQ";
        when(
            sentenceRepo.findById(sentenceId)
        ).thenReturn(
            Optional.of(
                loadSentenceFromFile(sentenceId)
            )
        );
        when(
            textRepo.findById(textId)
        ).thenReturn(
            Optional.of(
                loadTextFromFile(textId)
            )
        );
        mockMvc.perform(
            get(String.format("/sentence/get/%s", sentenceId)).contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            status().isOk()
        ).andExpect(
            content().contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            jsonPath(String.format("$.related.BTSText.%s.id", textId)).value(textId)
        );
    }

}