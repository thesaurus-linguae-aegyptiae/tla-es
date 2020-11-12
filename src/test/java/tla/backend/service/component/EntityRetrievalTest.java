package tla.backend.service.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tla.backend.es.model.parts.ObjectReference;
import tla.domain.model.meta.Resolvable;

public class EntityRetrievalTest {

    static List<Resolvable> refs = List.of(
        ObjectReference.builder().id("1").eclass("BTSText").build(),
        ObjectReference.builder().id("2").eclass("BTSThsEntry").build(),
        ObjectReference.builder().id("3").eclass("BTSThsEntry").build()
    );

    @Test
    void bulkRetrievalAdd() {
        var bulk = new EntityRetrieval.BulkEntityResolver();
        bulk.add(refs.get(1));
        bulk.add(refs.get(2));
        assertEquals(Map.of("BTSThsEntry", Set.of("2", "3")), bulk.refs);
    }

    @Test
    void bulkRetrievalAddAll() {
        var bulk = EntityRetrieval.BulkEntityResolver.of(List.of(refs.get(0)));
        assertTrue(bulk.refs.containsKey("BTSText"));
        bulk.addAll(
            List.of(refs.get(1), refs.get(2))
        );
        assertAll(
            () -> assertEquals(2, bulk.refs.keySet().size(), "2 eclasses"),
            () -> assertEquals(2, bulk.refs.get("BTSThsEntry").size(), "2 ths references")
        );
    }

}
