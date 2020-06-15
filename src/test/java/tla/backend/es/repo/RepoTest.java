package tla.backend.es.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ConversionException;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchDateConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import tla.backend.App;
import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.model.parts.EditDate;

@SpringBootTest(classes = {App.class})
public class RepoTest {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private ElasticsearchOperations operations;

    @Test
    void repoDoesExist() {
        assertTrue(repo != null, "repo should not be null");
    }

    @Test
    void esRestTemplateAvailable() {
        assertTrue(elasticsearchRestTemplate != null, "elasticsearch rest template should not be null");
    }

    @Test
    void indexLemmaSetup() {
        IndexOperations index = elasticsearchRestTemplate.indexOps(
            IndexCoordinates.of("lemma")
        );
        Map<?,?> lemmaIndexSettings = index.getSettings();
        assertAll("index lemma should exist and be configured correctly",
            () -> assertTrue(index.exists(), "index lemma should exist"),
            () -> assertTrue(!lemmaIndexSettings.isEmpty(), "lemma index should contain settings"),
            () -> assertTrue(lemmaIndexSettings.containsKey("index.max_result_window"), "settings should contain key `max_result_window`"),
            () -> assertEquals("100000", lemmaIndexSettings.get("index.max_result_window"))
        );
    }

    @Test
    void esDateConverterShouldParseLocalDate() {
        ElasticsearchDateConverter converter = ElasticsearchDateConverter.of("yyyy-MM-dd");
        EditDate localDate = converter.parse("2019-12-18", EditDate.class);
        assertAll("",
            () -> assertNotNull(localDate),
            () -> assertEquals("2019-12-18", localDate.toString())
        );
        assertThrows(DateTimeException.class,
            () -> converter.parse("2019-12-18")
        );
        assertThrows(ConversionException.class,
            () -> converter.parse("2019-12-18", LocalDate.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testObjectsInCollectionsMappings() {
        Map<String, Object> corpusobjectmap = operations.indexOps(CorpusObjectEntity.class).getMapping();
        assertAll("corpus object class ES mapping",
            () -> assertNotNull(corpusobjectmap, "corpus obj mapping"),
            () -> assertTrue(corpusobjectmap.get("properties") instanceof Map),
            () -> assertTrue(((Map<?,?>) corpusobjectmap.get("properties")).get("paths") instanceof Map)
        );
        Map<String, Object> pathsMap = (Map<String, Object>) ((Map<?,?>) corpusobjectmap.get("properties")).get("paths");
        assertFalse(pathsMap.isEmpty());

        /* // relations, paths, externalreferences index mappings still need fixing

        Map<String, Object> expectMap = Map.of("properties", Map.of());
        assertAll("paths mapping (objectreference)",
            () -> assertTrue(pathsMap.size() > 1, "contains more than just `type`"),
            () -> assertFalse(!pathsMap.containsKey("type"), "mapping has type"),
            () -> assertNotEquals(expectMap, pathsMap)
        ); */
    }
}
