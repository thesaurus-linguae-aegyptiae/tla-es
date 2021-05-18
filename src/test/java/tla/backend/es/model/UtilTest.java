package tla.backend.es.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class UtilTest {

    @Test
    void reverseList() {
        var list = List.of(1, 2, 3, 4);
        var reverse = Util.reverse(list);
        assertEquals(List.of(4, 3, 2, 1), reverse, "inverted list should be in inverse order");
    }

}
