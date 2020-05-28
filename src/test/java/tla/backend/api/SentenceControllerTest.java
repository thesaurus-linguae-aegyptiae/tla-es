package tla.backend.api;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import tla.backend.AbstractMockMvcTest;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.es.repo.TextRepo;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;


public class SentenceControllerTest extends AbstractMockMvcTest {

    @MockBean
    private SentenceRepo sentenceRepo;

    @MockBean
    private TextRepo textRepo;

    @Test
    void getDetails() throws Exception {
        String sentenceId = "IBUBd3QvPWhrgk50h3u3Wv5PmdA";
        String textId = "CDWYGHBII5C37IBETSSI6RCIDQ";
        when(
            sentenceRepo.findById(sentenceId)
        ).thenReturn(
            Optional.of(
                tla.domain.util.IO.loadFromFile(
                    String.format("src/test/resources/sample/sentence/%s.json", sentenceId),
                    SentenceEntity.class
                )
            )
        );
        when(
            textRepo.findById(textId)
        ).thenReturn(
            Optional.of(
                tla.domain.util.IO.loadFromFile(
                    String.format("src/test/resources/sample/text/%s.json", textId),
                    TextEntity.class
                )
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