package tla.backend.es.query;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;

public class SortSpecTest {

    @Test
    void testSearchSortSpec() {
        assertAll("sort spec from string",
            () -> assertEquals(SortOrder.ASC, SortSpec.from("sortKey_asc").order),
            () -> assertEquals("field_name", SortSpec.from("field_name_desc").field),
            () -> assertEquals(SortOrder.DESC, SortSpec.from("field_name_desc").order)
        );
    }

}