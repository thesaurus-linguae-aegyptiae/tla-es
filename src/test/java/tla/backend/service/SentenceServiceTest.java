package tla.backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import tla.backend.App;
import tla.backend.Util;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.AnnotationRepo;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.es.repo.TextRepo;
import tla.backend.es.repo.ThesaurusRepo;
import tla.domain.dto.extern.SingleDocumentWrapper;

@SpringBootTest(classes = {App.class})
public class SentenceServiceTest {

    @MockBean
    private SentenceRepo sentenceRepo;

    @MockBean
    private TextRepo textRepo;

    @MockBean
    private AnnotationRepo annoRepo;

    @MockBean
    private ThesaurusRepo thsRepo;

    @Autowired
    private SentenceService sentenceService;

    @Test
    void getSentenceDetailsWrapper() throws Exception {
        String textId = "CDWYGHBII5C37IBETSSI6RCIDQ";
        String sentenceId = "IBUBd3QvPWhrgk50h3u3Wv5PmdA";
        String annoId = "IBUBd0kXx8hvzU9vuxAKWNHnf6s";
        SentenceEntity s = Util.loadSampleFile(SentenceEntity.class, sentenceId);
        TextEntity t = Util.loadSampleFile(TextEntity.class, textId);
        AnnotationEntity rubrum = Util.loadSampleFile(AnnotationEntity.class, annoId);
        ThsEntryEntity tm = Util.loadSampleFile(ThsEntryEntity.class, "LJHMV4523JB7NBGAJRDTS3H27Y");
        when(
            thsRepo.findAllById(anyCollection())
        ).thenReturn(List.of(tm));
        when(
            sentenceRepo.findById(sentenceId)
        ).thenReturn(
            Optional.of(s)
        );
        when(
            textRepo.findById(textId)
        ).thenReturn(
            Optional.of(t)
        );
        when(
            textRepo.findAllById(anyCollection())
        ).thenReturn(
            List.of(t)
        );
        when(
            annoRepo.findById(annoId)
        ).thenReturn(
            Optional.of(rubrum)
        );
        SingleDocumentWrapper<?> result = sentenceService.getDetails(sentenceId);
        assertAll("result ok",
            () -> assertNotNull(result, "not null"),
            () -> assertNotNull(result.getDoc(), "payload found"),
            () -> assertNotNull(result.getRelated(), "related objects list"),
            () -> assertFalse(result.getRelated().isEmpty(), "contains related objects"),
            () -> assertTrue(result.getRelated().containsKey("BTSThsEntry"), "related objects contain thesaurus terms")
        );
    }

}
