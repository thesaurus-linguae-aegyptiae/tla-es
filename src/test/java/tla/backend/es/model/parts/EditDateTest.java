package tla.backend.es.model.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EditDateTest {

    @Test
    @DisplayName("editdates should be comparable")
    void shouldCompare() {
        assertTrue(
            EditDate.of(2019, 1, 15).compareTo(EditDate.of(2021, 1, 15)) < 1
        );
    }

    @Test
    @DisplayName("editdates representing same day should equal")
    void shouldEqual() {
        assertEquals(
            EditDate.of(2021, 1, 15), EditDate.of(2021, 1, 15)
        );
    }

    @Test
    @DisplayName("deserializing invalid date should throw exception")
    void invalidShouldThrow() {
        assertThrows(
            Exception.class,
            () -> EditDate.fromString("2021-00-15")
        );
    }

    @Test
    @DisplayName("deserializing undergranular date should throw exception")
    void undergranularShouldThrow() {
        assertThrows(
            Exception.class,
            () -> EditDate.fromString("2021-01")
        );
    }

    @Test
    @DisplayName("unsupported chrono field should always return 0")
    void invalidChronoFieldShouldDefault() {
        assertEquals(
            0, EditDate.of(2021, 1, 15).getLong(ChronoField.CLOCK_HOUR_OF_DAY)
        );
    }

}
