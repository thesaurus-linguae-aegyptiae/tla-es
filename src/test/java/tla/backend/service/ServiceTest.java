package tla.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;

import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tla.backend.App;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.ThsEntryEntity;

@SpringBootTest(classes = {App.class})
public class ServiceTest {

    @Autowired
    private LemmaService lemmaService;

    @Test
    void testServiceRegistry() {
        assertNotNull(lemmaService, "lemma service should be injected");
        Annotation lemmaServiceModelClassAnnotation = null;
        for (Annotation a : lemmaService.getClass().getAnnotations()) {
            lemmaServiceModelClassAnnotation = (a instanceof ModelClass) ? a : lemmaServiceModelClassAnnotation;
        }
        assertNotNull(lemmaServiceModelClassAnnotation, "lemma service should have expected annotation");
        assertAll("test if services register themselves",
            () -> assertTrue(EntityService.modelClassServices.size() > 0),
            () -> assertTrue(EntityService.modelClassServices.containsKey(LemmaEntity.class)),
            () -> assertTrue(EntityService.modelClassServices.containsKey(ThsEntryEntity.class)),
            () -> assertTrue(EntityService.modelClassServices.containsKey(AnnotationEntity.class)),
            () -> assertTrue(EntityService.getRegisteredModelClasses().size() > 0)
        );
    }

    @Test
    void testSearchSortSpec() {
        assertAll("sort spec from string",
            () -> assertEquals(SortOrder.ASC, LemmaService.SortSpec.from("sortKey_asc").order),
            () -> assertEquals("field_name", LemmaService.SortSpec.from("field_name_desc").field),
            () -> assertEquals(SortOrder.DESC, LemmaService.SortSpec.from("field_name_desc").order)
        );
    }
}