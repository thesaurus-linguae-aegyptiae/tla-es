package tla.backend.es.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.annotations.Document;

import tla.backend.App;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.model.parts.EditDate;
import tla.backend.es.model.parts.EditorInfo;
import tla.backend.es.model.parts.LemmaWord;
import tla.backend.es.model.parts.PartOfSpeech;
import tla.backend.es.model.parts.Token;
import tla.backend.es.model.parts.Transcription;
import tla.backend.es.model.parts.Translations;
import tla.backend.es.model.parts.Token.Flexion;
import tla.backend.es.model.parts.Token.Lemmatization;
import tla.domain.dto.AnnotationDto;
import tla.domain.dto.CorpusObjectDto;
import tla.domain.dto.meta.DocumentDto;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.SentenceDto;
import tla.domain.dto.TextDto;
import tla.domain.dto.ThsEntryDto;
import tla.domain.model.Language;
import tla.domain.model.Passport;
import tla.domain.model.SentenceToken;
import tla.domain.model.meta.BTSeClass;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

@SpringBootTest(classes = {App.class})
public class ModelTest {

    private ObjectMapper mapper = new ObjectMapper();

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
        Translations t1 = Translations.builder().de(List.of("übersetzung")).en(List.of("translation", "meaning")).build();
        Translations t2 = Translations.builder().de(Arrays.asList("übersetzung")).en(Arrays.asList("translation", "meaning")).build();
        Translations t3 = mapper.readValue("{\"de\": [\"übersetzung\"], \"en\": [\"translation\", \"meaning\"]}", Translations.class);
        assertAll("translations objects should be equal",
            () -> assertEquals(t2, t1, "translation instances should be equal regardless of build method parameter type"),
            () -> assertEquals(t3, t1, "deserialized instance should be equal to builder-instantiated"),
            () -> assertNull(t1.getFr(), "builder-built french translations array should be null"),
            () -> assertNull(t3.getFr(), "deserialized french translations array should be null"),
            () -> assertEquals(t3.hashCode(), t1.hashCode(), "hashcodes equal"),
            () -> assertNull(t3.get(null), "undefined language causes null")
        );
    }

    @Test
    void editInfoEqual() throws Exception {
        EditorInfo e1 = mapper.readValue(
            "{\"author\":\"author\", \"updated\":\"2019-12-18\"}",
            EditorInfo.class
        );
        EditorInfo e2 = mapper.readValue(
            mapper.writeValueAsString(e1),
            EditorInfo.class
        );
        assertAll("test EditorInfo equality",
            () -> assertEquals(e1, e2, "instances as a whole should be equal"),
            () -> assertEquals(e1.getUpdated(), e2.getUpdated(), "edit dates should be equals")
        );
    }

    @Test
    void thesaurusEntriesEqual() throws Exception {
        ThsEntryEntity t_built = ThsEntryEntity.builder()
            .id("1")
            .eclass("BTSThsEntry")
            .sortKey("1")
            .editors(EditorInfo.builder().author("author").updated(EditDate.of(2015, 12, 31)).build())
            .build();
        ThsEntryEntity t_read = mapper.readValue(
            "{\"id\":\"ID\",\"eclass\":\"BTSThsEntry\",\"sort_string\":\"1\",\"editors\":{\"author\":\"author\",\"updated\":\"2015-12-31\"}}",
            ThsEntryEntity.class
        );
        ThsEntryEntity t_round = mapper.readValue(mapper.writeValueAsString(t_built), ThsEntryEntity.class);
        assertAll("thesaurus entry instances should be equal regardless of creation method",
            () -> assertNotEquals(t_built, t_read, "deserialized instance should not be the same as built instance"),
            () -> assertEquals(t_built, t_round, "built instance should remain the same after serialization and deserialization via ES entity mapper"),
            () -> assertEquals(t_built.getEditors(), t_read.getEditors(), "edit infos should be equal")
        );
    }

    @Test
    void thesrausEntryDeserialize_mapping() throws Exception {
        ThsEntryEntity t = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/ths/E7YEQAEKZVEJ5PX7WKOXY2QEEM.json",
            ThsEntryEntity.class
        );
        assertAll("synonyms should be extracted from passport and converted into translations",
            () -> assertNotNull(t.getTranslations()),
            () -> assertNotNull(t.getTranslations().getFr()),
            () -> assertEquals(1, t.getTranslations().getFr().size()),
            () -> assertEquals("21e dynastie", t.getTranslations().getFr().get(0))
        );
        DocumentDto dto = (DocumentDto) t.toDTO();
        assertAll("test thesaurus entity to DTO mapping",
            () -> assertTrue(dto instanceof ThsEntryDto),
            () -> assertNotNull(dto.getReviewState(), "review status must not be null"),
            () -> assertNotNull(((ThsEntryDto) dto).getTranslations(), "translations must not be null")
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
        LemmaEntity l_read = mapper.readValue(
            "{\"id\":\"1\",\"passport\":{}}",
            LemmaEntity.class
        );
        LemmaEntity l_round = mapper.readValue(
            mapper.writeValueAsString(l_built),
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
            .translations(Translations.builder().de(List.of("übersetzung")).build())
            .word(
                new LemmaWord(
                    "N35:G47",
                    new Transcription("nfr", "nfr")
                )
            )
            .build();
        DocumentDto dto = (DocumentDto) l.toDTO();
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
    void annotationModelMapping() throws Exception {
        AnnotationEntity a = AnnotationEntity.builder()
            .id("ID")
            .eclass("BTSAnnotation")
            .name("nfr")
            .revisionState("published")
            .passport(Passport.of(Map.of("lemma", Collections.emptyMap())))
            .build();
        AnnotationDto d = AnnotationDto.class.cast(a.toDTO());
        assertAll("test annotation entity to DTO conversion",
            () -> assertEquals(a.getEclass(), d.getEclass()),
            () -> assertEquals(a.getName(), d.getName()),
            () -> assertEquals(a.getPassport(), d.getPassport()),
            () -> assertEquals(a.getRevisionState(), d.getReviewState()),
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
        AnnotationEntity b = mapper.readValue(
            mapper.writeValueAsString(a),
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
        DocumentDto d = (DocumentDto) t.toDTO();
        assertAll("test text to DTO mapping",
            () -> assertNotNull(d),
            () -> assertTrue(d instanceof TextDto),
            () -> assertEquals(t.getRevisionState(), d.getReviewState())
        );
        TextDto dto = (TextDto) d;
        assertAll("test mapped text DTO properties",
            () -> assertNotNull(dto.getPaths())
        );
    }

    @Test
    void corpusobjectFromFileMapping() throws Exception {
        CorpusObjectEntity o = tla.domain.util.IO.loadFromFile(
            "src/test/resources/sample/object/2ABHCXHF5BCG3PDW7VSPA77K7U.json",
            CorpusObjectEntity.class
        );
        assertAll("test corpus object deserialization from JSON file",
            () -> assertNotNull(o),
            () -> assertNotNull(o.getEditors(), "editor info not null"),
            () -> assertNotNull(o.getEditors().getUpdated(), "edit date no null"),
            () -> assertNotNull(o.getPaths()),
            () -> assertEquals("bbawarchive", o.getCorpus())
        );
        DocumentDto d = (DocumentDto) o.toDTO();
        assertAll("test corpus object to DTO mapping",
            () -> assertNotNull(d),
            () -> assertTrue(d instanceof CorpusObjectDto),
            () -> assertNotNull(((CorpusObjectDto) d).getPaths()),
            () -> assertEquals(o.getRevisionState(), d.getReviewState()),
            () -> assertNotNull(d.getEditors(), "DTO edit info not null"),
            () -> assertNotNull(d.getEditors().getUpdated(), "DTO edit date not null")
        );
    }

    @Test
    void commentDeserializeSimple() throws Exception {
        CommentEntity c = mapper.readValue(
            "{\"eclass\":\"BTSComment\",\"id\":\"ID\",\"body\":\"comment\",\"relations\":{\"partOf\":[{\"id\":\"1\",\"eclass\":\"BTSText\",\"ranges\":[{\"from\":\"a\",\"to\":\"b\"}]}]}}",
            CommentEntity.class
        );
        assertAll("comment deserialization ok",
            () -> assertNotNull(c, "instance not null"),
            () -> assertNotNull(c.getBody(), "body"),
            () -> assertEquals("comment", c.getBody(), "content"),
            () -> assertNotNull(c.getRelations(), "relations"),
            () -> assertEquals(1, c.getRelations().size(), "relation predicate count"),
            () -> assertNotNull(c.getRelations().get("partOf"), "predicate partof"),
            () -> assertEquals(1, c.getRelations().get("partOf").size(), "1 relation"),
            () -> assertNotNull(c.getRelations().get("partOf").get(0), "first relation"),
            () -> assertNotNull(c.getRelations().get("partOf").get(0).getRanges(), "text range"),
            () -> assertTrue(!c.getRelations().get("partOf").get(0).getRanges().isEmpty(), "text range"),
            () -> assertNotNull(c.getRelations().get("partOf").get(0).getRanges().get(0).getFrom(), "text range boundary")
        );
        CommentEntity c2 = CommentEntity.builder()
            .id(c.getId())
            .eclass(c.getEclass())
            .body(c.getBody())
            .relations(c.getRelations())
            .build();
        assertAll("equality",
            () -> assertEquals(c, c2, "instances"),
            () -> assertEquals(c.toString(), c2.toString(), "str repr"),
            () -> assertEquals(c.hashCode(), c2.hashCode(), "hashcode")
        );
    }

    @Test
    void mapSentenceToDTO() throws Exception {
        Flexion f = new Flexion();
        Lemmatization l = new Lemmatization();
        SentenceEntity.Context c = SentenceEntity.Context.builder()
            .textId("textId").line("[1]").pos(0).build();
        l.setPos(new PartOfSpeech("substantive", "masc"));
        f.setNumeric(3L);
        Token t = new Token();
        t.setFlexion(f);
        t.setLemma(l);
        t.setTranslations(Translations.builder().de(List.of("bedeutung")).build());;
        SentenceEntity s = SentenceEntity.builder()
            .id("ID")
            .context(c)
            .transcription(new Transcription("nfr", "nfr"))
            .translations(Translations.builder().de(List.of("uebersetzung")).build())
            .tokens(List.of(t))
            .build();
        SentenceDto dto = (SentenceDto) ModelConfig.toDTO(s);
        assertAll("test sentence entity to DTO mapping",
            () -> assertNotNull(dto, "instance"),
            () -> assertNotNull(dto.getTranscription(), "transcription"),
            () -> assertEquals("nfr", dto.getTranscription().getUnicode(), "transcription unicode"),
            () -> assertNotNull(dto.getTranslations(), "translations"),
            () -> assertTrue(dto.getTranslations().containsKey(Language.DE), "german translation"),
            () -> assertEquals(List.of("uebersetzung"), dto.getTranslations().get(Language.DE), "translation value"),
            () -> assertNotNull(dto.getTokens(), "tokens"),
            () -> assertEquals(1, dto.getTokens().size(), "1 token"),
            () -> assertNotNull(dto.getContext(), "sentence context in DTO"),
            () -> assertEquals(s.getContext().getLine(), dto.getContext().getLine(), "lc")
        );
        SentenceToken tdto = dto.getTokens().get(0);
        assertAll("test sentence token to DTO mapping",
            () -> assertNotNull(tdto, "token"),
            () -> assertNotNull(tdto.getFlexion(), "flexion"),
            () -> assertEquals(3L, tdto.getFlexion().getNumeric(), "flexcode"),
            () -> assertNotNull(tdto.getLemma(), "lemmatization")
        );
    }

}
