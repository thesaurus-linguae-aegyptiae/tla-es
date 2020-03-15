package tla.backend.es.model;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.EntityMapper;

import tla.backend.App;
import tla.backend.Util;
import tla.domain.dto.LemmaDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.Passport;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = {App.class})
public class ModelTest {

    @Autowired
    private EntityMapper mapper;

    @Autowired
    private ModelMapper modelMapper;

    @Test
    void entitySuperClass_equality() throws Exception {
        IndexedEntity lemma = LemmaEntity.builder().id("ID").build();
        IndexedEntity term = ThsEntryEntity.builder().id("ID").build();
        assertAll("entities of different subclass with same ID should not be equal",
            () -> assertTrue(!lemma.equals(term), "lemma 'ID' should not equal ths term 'ID'")
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

    public static ThsEntryEntity baseThsEntry() {
        return ThsEntryEntity.builder()
            .id("1")
            .name("wadi")
            .type("findSpot")
            .relation("contains", Arrays.asList(
                ObjectReference.builder().id("2").name("region1").type("findSpot").build(),
                ObjectReference.builder().id("3").name("region2").type("findSpot").build()
            ))
            .build();
    }

    @Test
    void thesaurusEntriesEqual() throws Exception {
        ThsEntryEntity t_built = ThsEntryEntity.builder()
            .id("1")
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
    void btsAnnotatedEntitiesShouldAlwaysReturnEclass() throws Exception {
        assertAll("returned eclass values should be as defined",
            () -> assertEquals("BTSText", (new TextEntity()).getEclass(), "text entity eclass should be `BTSText"),
            () -> assertEquals("BTSLemmaEntry", (new LemmaEntity()).getEclass(), "lemma eclass should be `BTSLemmaEntry`"),
            () -> assertEquals("BTSThsEntry", (new ThsEntryEntity()).getEclass(), "ths term eclass should be `BTSThsEntry`"),
            () -> assertEquals(
                "tla.backend.es.model.OccurrenceEntity",
                (new OccurrenceEntity()).getEclass(),
                "class name should be returned instead of eclass for occurrence"
            )
        );
    }

    @Test
    void nonNullFieldValidation() {
        assertThrows(NullPointerException.class,
            () -> {LemmaEntity.builder().build();},
            "building lemma with null-ID should throw exception"
        );
    }

    @Test
    void lemmaEntriesEqual() throws Exception {
        LemmaEntity l_built = LemmaEntity.builder()
            .id("1")
            .passport(new Passport())
            .build();
        LemmaEntity l_read = mapper.mapToObject(
            "{\"id\":\"1\",\"eclass\":\"BTSLemmaEntry\",\"passport\":{}}",
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

    public static LemmaEntity baseLemma() {
        return LemmaEntity.builder()
        .id("Id")
        .name("nfr")
        .type("subst")
        .revisionState("published")
        .sortKey("Id")
        .translations(Translations.builder().de("übersetzung").build())
        .build();
    }

    @Test
    void lemmaModelMapping() {
        LemmaEntity l = baseLemma();
        LemmaDto d = modelMapper.map(l, LemmaDto.class);
        assertAll("lemma entity should be mapped to DTO correctly",
            () -> assertEquals(l.getRevisionState(), d.getReviewState(), "review status should be present"),
            () -> assertEquals(l.getSortKey(), d.getSortKey(), "sort key should be copied"),
            () -> assertTrue(!d.getTranslations().isEmpty(), "translations should not be empty")
        );
    }

    public static TextEntity baseText() {
        return TextEntity.builder()
        .id("2")
        .corpus("corpus")
        .paths(
            List.of(
                List.of(
                    ObjectReference.builder().id("1").eclass("BTSTCObject").type("o").name("n").build()
                )
            )
        ).name("nn")
        .type("t")
        .build();
    }

    @Test
    void textEquality() throws Exception {
        TextEntity t1 = baseText();
        TextEntity t2 = mapper.mapToObject(
            "{\"id\":\"2\",\"name\":\"nn\",\"corpus\":\"corpus\",\"type\":\"t\",\"paths\":[[{\"id\":\"1\",\"eclass\":\"BTSTCObject\",\"type\":\"o\",\"name\":\"n\"}]]}",
            TextEntity.class
        );
        assertAll("test text instances for equality",
            () -> assertEquals(t1, t2, "deserialized instance should be the same as builder built"),
            () -> assertEquals(t1.toString(), t2.toString(), "tostring repr should be the same"),
            () -> assertEquals(t1.hashCode(), t2.hashCode(), "hashcodes should be the same")
        );
    }

}