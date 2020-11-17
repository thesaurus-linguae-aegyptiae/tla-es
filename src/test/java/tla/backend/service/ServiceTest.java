package tla.backend.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tla.backend.App;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.ExternalReference;

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
    void dtoBatchMapping() throws Exception {
        LemmaEntity l = tla.domain.util.IO.loadFromFile("src/test/resources/sample/lemma/31610.json", LemmaEntity.class);
        LemmaEntity l2 = LemmaEntity.builder().id("1")
            .externalReference("thor", List.of(
                ExternalReference.builder().id("thot-1").build(),
                ExternalReference.builder().id("thot-2").type("topbib").build()
            )).name("nfr-nfr-nfr")
            .type("substantive")
            .build();
        Collection<? extends AbstractDto> dto = lemmaService.toDTO(
            List.of(l, l2)
        );
        assertNotNull(dto);
    }

}