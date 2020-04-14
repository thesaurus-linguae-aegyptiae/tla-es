package tla.backend.es.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.EntityMapper;

import tla.backend.App;
import tla.backend.Util;
import tla.domain.dto.AnnotationDto;
import tla.domain.dto.DocumentDto;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.TextDto;
import tla.domain.model.Passport;
import tla.domain.model.meta.BTSeClass;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

@SpringBootTest(classes = {App.class})
public class ModelTest {

    @Autowired
    private EntityMapper mapper;

    @Test
    void modelConfigInitialized() {
        List<Class<? extends BaseEntity>> modelClasses = ModelConfig.getModelClasses();
        assertAll("make sure model config class has been initialized",
            () -> assertTrue(ModelConfig.isInitialized(), "flag should be set"),
            () -> assertNotNull(modelClasses, "model class list should not be null"),
            () -> assertNotNull(ModelConfig.getModelClassConfigs(), "model configurations registry expected"),
            () -> assertEquals(
                modelClasses.size(),
                ModelConfig.getModelClassConfigs().size(),
                String.format(
                    "number of model class configurations registered should be the same as model classes known to ModelConfig (%s)",
                    String.join(
                        ", ",
                        modelClasses.stream().map(Class::getName).collect(Collectors.toList())
                    )
                )
            )
        );
    }

    @BTSeClass("BTSMadeUpModel")
    @Document(indexName = "made_up_index_name")
    private static class CorrectlyAnnotatedDummyModelClass extends TLAEntity {}

    @Document(indexName = "made_up_index_name")
    private static class IncorrectlyAnnotatedDummyModelClass extends TLAEntity {}

    @Test
    void registerModelClass() throws Exception {
        int numberOfRegisteredModels = ModelConfig.getModelClasses().size();
        int numberOfRegisteredModelConfigs = ModelConfig.getModelClassConfigs().size();
        try {
            Map<String, ModelConfig.BTSeClassConfig> conf = ModelConfig.registerModelClass(
                CorrectlyAnnotatedDummyModelClass.class
            );
            assertAll("model class config should be extracted and registered",
                () -> assertNotNull(conf, "configuration expected"),
                () -> assertTrue(conf.containsKey("BTSMadeUpModel"), "expect eclass"),
                () -> assertEquals("made_up_index_name", conf.get("BTSMadeUpModel").getIndex(), "expect index name"),
                () -> assertEquals(CorrectlyAnnotatedDummyModelClass.class, conf.get("BTSMadeUpModel").getModelClass(), "expect class"),
                () -> assertEquals(numberOfRegisteredModels + 1, ModelConfig.getModelClasses().size(), "expect one more known model class"),
                () -> assertEquals(numberOfRegisteredModelConfigs + 1, ModelConfig.getModelClassConfigs().size(), "expect one more registered config"),
                () -> assertEquals(conf.get("BTSMadeUpModel").getIndex(), ModelConfig.getIndexName(CorrectlyAnnotatedDummyModelClass.class)),
                () -> assertEquals(CorrectlyAnnotatedDummyModelClass.class, ModelConfig.getModelClass("BTSMadeUpModel"))
            );
        } catch (Exception e) {
            //log.warn("model class registration failed for {}", CorrectlyAnnotatedDummyModelClass.class.getName());
            throw e;
        }
    }

    @Test
    void registerInvalidModelClass() {
        assertThrows(
            Exception.class,
            () -> ModelConfig.registerModelClass(
                IncorrectlyAnnotatedDummyModelClass.class
            )
        );
    }

    @Test
    void lookupModelClassForUnknownEclass() {
        assertThrows(
            NullPointerException.class,
            () -> ModelConfig.getModelClass("nonexistentEclass")
        );
    }

    @Test
    void entitySuperClass_equality() throws Exception {
        Indexable lemma = LemmaEntity.builder().id("ID").build();
        Indexable term = ThsEntryEntity.builder().id("ID").build();
        assertAll("entities of different subclass with same ID should not be equal",
            () -> assertNotEquals(lemma, term, "lemma 'ID' should not equal ths term 'ID'")
        );
    }

    @Test
    void translationsEqual() throws Exception {
        assertTrue(mapper != null, "entitymapper should not be null");
        Translations t1 = Translations.builder().de("übersetzung").en("translation").en("meaning").build();
        Translations t2 = Translations.builder().de(Arrays.asList("übersetzung")).en(Arrays.asList("translation", "meaning")).build();
        Translations t3 = mapper.mapToObject("{\"de\": [\"übersetzung\"], \"en\": [\"translation\", \"meaning\"]}", Translations.class);
        assertAll("translations objects should be equal",
            () -> assertEquals(t2, t1, "translation instances should be equal regardless of build method parameter type"),
            () -> assertEquals(t3, t1, "deserialized instance should be equal to builder-instantiated"),
            () -> assertTrue(t1.getFr().isEmpty(), "builder-built french translations array should be empty"),
            () -> assertTrue(t3.getFr().isEmpty(), "deserialized french translations array should be empty")
        );
    }

    @Test
    void thesaurusEntriesEqual() throws Exception {
        ThsEntryEntity t_built = ThsEntryEntity.builder()
            .id("1")
            .eclass("BTSThsEntry")
            .sortKey("1")
            .editors(EditorInfo.builder().author("author").updated(Util.date("2015-12-31")).build())
            .build();
        ThsEntryEntity t_read = mapper.mapToObject(
            "{\"id\":\"ID\",\"eclass\":\"BTSThsEntry\",\"sort_string\":\"1\",\"editors\":{\"author\":\"author\",\"updated\":\"2015-12-31\"}}",
            ThsEntryEntity.class
        );
        ThsEntryEntity t_round = mapper.mapToObject(mapper.mapToString(t_built), ThsEntryEntity.class);
        assertAll("thesaurus entry instances should be equal regardless of creation method",
            () -> assertNotEquals(t_built, t_read, "deserialized instance should not be the same as built instance"),
            () -> assertEquals(t_built, t_round, "built instance should remain the same after serialization and deserialization via ES entity mapper"),
            () -> assertEquals(t_built.getEditors(), t_read.getEditors(), "edit infos should be equal")
        );
    }

    @Test
    void lemmaRegistered() throws Exception {
        assertAll("has lemma model class registered with modelconfig?",
            () -> assertEquals(LemmaEntity.class, ModelConfig.getModelClass("BTSLemmaEntry"), "check registered model class"),
            () -> assertEquals("BTSLemmaEntry", ModelConfig.getEclass(LemmaEntity.class), "check registered eclass"),
            () -> assertEquals("lemma", ModelConfig.getIndexName(LemmaEntity.class), "cehck lemma index name")
        );
    }

    @Test
    void lemmaEntriesEqual() throws Exception {
        LemmaEntity l_built = LemmaEntity.builder()
            .id("1")
            .passport(new Passport())
            .build();
        LemmaEntity l_read = mapper.mapToObject(
            "{\"id\":\"1\",\"passport\":{}}",
            LemmaEntity.class
        );
        LemmaEntity l_round = mapper.mapToObject(
            mapper.mapToString(l_built),
            LemmaEntity.class
        );
        assertAll("lemma entry instances should be equal regardless of creation method",
            () -> assertEquals("BTSLemmaEntry", l_built.getEclass(), "superclass getEclass() method should return registered eClass value"),
            () -> assertEquals(l_built, l_read, "deserialized lemma instance should be equal to built instance with the same properties"),
            () -> assertEquals(l_built, l_round, "lemma instance serialized and then deserialized should equal itself")
        );
    }

    @Test
    void deserializeLemma() throws Exception {
        LemmaEntity l = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/lemma/31610.json",
            LemmaEntity.class
        );
        assertAll("deserialize lemma entity from JSON file",
            () -> assertNotNull(l),
            () -> assertNotNull(l.getPassport())
        );
    }

    @Test
    void lemmaModelMapping() {
        LemmaEntity l = LemmaEntity.builder()
            .id("Id")
            .eclass("BTSLemmaEntry")
            .name("nfr")
            .type("subst")
            .revisionState("published")
            .sortKey("Id")
            .translations(Translations.builder().de("übersetzung").build())
            .word(
                new LemmaWord(
                    "N35:G47",
                    new Transcription("nfr", "nfr")
                )
            )
            .build();
        DocumentDto dto = l.toDTO();
        assertTrue(dto instanceof LemmaDto);
        LemmaDto d = (LemmaDto) dto;
        assertAll("lemma entity should be mapped to DTO correctly",
            () -> assertEquals(l.getRevisionState(), d.getReviewState(), "review status should be present"),
            () -> assertEquals(l.getSortKey(), d.getSortKey(), "sort key should be copied"),
            () -> assertTrue(!d.getTranslations().isEmpty(), "translations should not be empty"),
            () -> assertEquals(1, l.getWords().size(), "expect 1 word"),
            () -> assertNotNull(d.getWords().get(0).getTranscription(), "word should have transcription")
        );
    }

    @Test
    void annotationClassRegistered() {
        assertAll("annotation model class registered?",
            () -> assertEquals("BTSAnnotation", ModelConfig.getEclass(AnnotationEntity.class)),
            () -> assertEquals(AnnotationEntity.class, ModelConfig.getModelClass("BTSAnnotation")),
            () -> assertEquals("annotation", ModelConfig.getIndexName(AnnotationEntity.class))
        );
    }

    @Test
    void annotationModelMapping() {
        AnnotationEntity a = AnnotationEntity.builder()
            .id("ID")
            .eclass("BTSAnnotation")
            .name("nfr")
            .revisionState("published")
            .passport(Passport.of(Map.of("lemma", Collections.emptyMap())))
            .build();
        DocumentDto d = a.toDTO();
        assertAll("test annotation entity to DTO conversion",
            () -> assertEquals(d.getEclass(), a.getEclass()),
            () -> assertEquals(d.getName(), a.getName()),
            () -> assertEquals(d.getPassport(), a.getPassport()),
            () -> assertTrue(d instanceof AnnotationDto)
        );
    }

    @Test
    void annotationDeserializeFromFile() throws Exception {
        AnnotationEntity a = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/annotation/2Y6NIZZWUJG7XAT3Y63A6WICA4.json",
            AnnotationEntity.class
        );
        assertAll("test annotation deserialization from JSON file",
            () -> assertNotNull(a),
            () -> assertEquals("Annotation zu $jzr$", a.getName()),
            () -> assertEquals(1, a.getPassport().extractProperty("annotation.lemma").size()),
            () -> assertTrue(a.getPassport().extractProperty("annotation.lemma").get(0).getLeafNodeValue().length() > 100)
        );
        AnnotationEntity b = mapper.mapToObject(
            mapper.mapToString(a),
            AnnotationEntity.class
        );
        assertAll("after serialization and deserialization, annotation object should be still the same",
            () -> assertEquals(a, b),
            () -> assertEquals(a.hashCode(), b.hashCode()),
            () -> assertEquals(a.toString(), b.toString())
        );
    }

    @Test
    void textDeserializeFromFile() throws Exception {
        TextEntity t = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/text/2A5EGGJVHVFVVL42QSWVLJORYE.json",
            TextEntity.class
        );
        assertAll("test text deserialization from JSON file",
            () -> assertNotNull(t),
            () -> assertNotNull(t.getPaths()),
            () -> assertEquals("bbawarchive", t.getCorpus())
        );
    }

    @Test
    void textMapping() throws Exception {
        TextEntity t = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/text/2A5EGGJVHVFVVL42QSWVLJORYE.json",
            TextEntity.class
        );
        DocumentDto d = t.toDTO();
        assertAll("test text to DTO mapping",
            () -> assertNotNull(d),
            () -> assertTrue(d instanceof TextDto)
        );
        TextDto dto = (TextDto) d;
        assertAll("test mapped text DTO properties",
            () -> assertNotNull(dto.getPaths())
        );
    }

}
