package tla.backend.es.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.EntityMapper;

import tla.backend.App;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

@SpringBootTest(classes = {App.class})
public class ModelTest {

    @Autowired
    private EntityMapper mapper;

    @Test
    void translationsEqual() throws Exception {
        assertTrue(mapper != null, "entitymapper should not be null");
        Translations t1 = Translations.builder().de("übersetzung").build();
        Translations t2 = Translations.builder().de(Arrays.asList("übersetzung")).build();
        Translations t3 = mapper.mapToObject("{\"de\": [\"übersetzung\"]}", Translations.class);
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
            .sortKey("1")
            .build();
        ThsEntryEntity t_read = mapper.mapToObject("{\"id\":\"ID\",\"sort_string\":\"1\"}", ThsEntryEntity.class);
        ThsEntryEntity t_round = mapper.mapToObject(mapper.mapToString(t_built), ThsEntryEntity.class);
        assertAll("thesaurus entry instances should be equal regardless of creation method",
            () -> assertEquals(t_built, t_read, "deserialized instance should be the same as built instance"),
            () -> assertEquals(t_built, t_round, "built instance should remain the same after serialization and deserialization via ES entity mapper")
        );
    }

}