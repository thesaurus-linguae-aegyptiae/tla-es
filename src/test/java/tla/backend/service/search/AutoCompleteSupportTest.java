package tla.backend.service.search;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class AutoCompleteSupportTest {

    @Test
    void testBoostCalculation() {
        AutoCompleteSupport ac = AutoCompleteSupport.builder()
            .queryField("name", 1F)
            .responseFields(new String[]{"name"})
            .build();
        assertAll("test custom autocomplete support multimatch query configurations",
            () -> assertEquals(2F, ac.getQueryFields().get("name"), "extra boost on name field"),
            () -> assertEquals(
                Map.of(
                    "name._3gram", 1F,
                    "name._2gram", 1F,
                    "name", 2F,
                    "id", 1F
                ),
                ac.getQueryFields(),
                "field boost settings"
            ),
            () -> assertFalse(
                Arrays.stream(ac.getResponseFields()).distinct().count()
                < Arrays.stream(ac.getResponseFields()).count(),
                "expect no duplicate of search request source field `name`"
            )
        );
    }

}