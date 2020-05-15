package tla.backend.es.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchDateConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import tla.backend.App;
import tla.backend.es.model.parts.EditDate;

@SpringBootTest(classes = {App.class})
public class RepoTest {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

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
    }
}