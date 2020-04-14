package tla.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import tla.backend.App;

@SpringBootTest(classes = {App.class})
public class ServiceTest {

    @Test
    void testServiceRegistry() {
        assertAll("test if services register themselves",
            () -> assertTrue(QueryService.eclassServices.size() > 0),
            () -> assertTrue(QueryService.eclassServices.containsKey("BTSLemmaEntry")),
            () -> assertTrue(QueryService.eclassServices.containsKey("BTSThsEntry")),
            () -> assertTrue(QueryService.eclassServices.containsKey("BTSAnnotation"))
        );
    }
}