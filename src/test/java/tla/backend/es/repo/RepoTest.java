package tla.backend.es.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import tla.backend.App;

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
        Map<?,?> lemmaIndexSettings = elasticsearchRestTemplate.getSetting("lemma");
        assertAll("index lemma should exist and be configured correctly",
            () -> assertTrue(elasticsearchRestTemplate.indexExists("lemma"), "index lemma should exist"),
            () -> assertTrue(!lemmaIndexSettings.isEmpty(), "lemma index should contain settings"),
            () -> assertTrue(lemmaIndexSettings.containsKey("index.max_result_window"), "settings should contain key `max_result_window`"),
            () -> assertEquals("100000", lemmaIndexSettings.get("index.max_result_window"))
        );
    }

}