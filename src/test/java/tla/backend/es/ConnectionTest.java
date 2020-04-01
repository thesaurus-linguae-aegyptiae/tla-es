package tla.backend.es;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionTest {

    @Test
    void envSet() {
        String esPort = System.getenv("ES_PORT");
        assertTrue(esPort != null, "ES_PORT should be set");
    }

    private URL getElasticsearchURL(String path) throws Exception {
        return new URL(
            String.format(
                "http://localhost:%s/%s",
                System.getenv("ES_PORT"),
                path
            )
        );
    }

    @Test
    void doesESrespond() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) getElasticsearchURL(
            ""
        ).openConnection();
        int responseStatus = connection.getResponseCode();
        assertEquals(200, responseStatus, "ES should return HTTP code 200");
    }

    @Test
    void doesLemmaIndexExist() throws Exception {
        assertAll("GET requests to indices should respond as expected",
            () -> assertEquals(
                200,
                ((HttpURLConnection) getElasticsearchURL(
                    "_cat/indices/lemma"
                ).openConnection()).getResponseCode(),
                "_cat call for lemma index should return HTTP 200"
            ),
            () -> assertEquals(
                404,
                ((HttpURLConnection) getElasticsearchURL(
                    "_cat/indices/xxx"
                ).openConnection()).getResponseCode(),
                "_cat call for non-existent index should return HTTP 404"
            )
        );
    }

}