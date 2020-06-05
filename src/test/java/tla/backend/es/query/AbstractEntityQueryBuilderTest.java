package tla.backend.es.query;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;

public class AbstractEntityQueryBuilderTest {

    @Test
    void testSearchSortSpec() {
        assertAll("sort spec from string",
            () -> assertEquals(SortOrder.ASC, AbstractEntityQueryBuilder.SortSpec.from("sortKey_asc").order),
            () -> assertEquals("field_name", AbstractEntityQueryBuilder.SortSpec.from("field_name_desc").field),
            () -> assertEquals(SortOrder.DESC, AbstractEntityQueryBuilder.SortSpec.from("field_name_desc").order)
        );
    }

}