package tla.backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import tla.backend.App;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.es.repo.TextRepo;
import tla.domain.dto.extern.SingleDocumentWrapper;

@SpringBootTest(classes = {App.class})
public class SentenceServiceTest {
    
    @MockBean
    private SentenceRepo sentenceRepo;

    @MockBean
    private TextRepo textRepo;

    @Autowired
    private SentenceService sentenceService;

    @Test
    void getSentenceDetailsWrapper() throws Exception {
        String textId = "CDWYGHBII5C37IBETSSI6RCIDQ";
        String sentenceId = "IBUBd3QvPWhrgk50h3u3Wv5PmdA";
        SentenceEntity s = tla.domain.util.IO.loadFromFile(
            String.format("src/test/resources/sample/sentence/%s.json", sentenceId),
            SentenceEntity.class
        );
        TextEntity t = tla.domain.util.IO.loadFromFile(
            String.format("src/test/resources/sample/text/%s.json", textId),
            TextEntity.class
        );
        when(
            sentenceRepo.findById(sentenceId)
        ).thenReturn(
            Optional.of(
                s
            )
        );
        when(
            textRepo.findById(textId)
        ).thenReturn(
            Optional.of(
                t
            )
        );
        SingleDocumentWrapper<?> result = sentenceService.getDetails(sentenceId);
        assertAll("result ok",
            () -> assertNotNull(result, "not null"),
            () -> assertNotNull(result.getDoc(), "payload found"),
            () -> assertNotNull(result.getRelated(), "related objects list"),
            () -> assertFalse(result.getRelated().isEmpty(), "contains related objects")
        );
    }

}
