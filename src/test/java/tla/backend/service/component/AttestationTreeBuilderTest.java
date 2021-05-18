package tla.backend.service.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tla.backend.Util;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.meta.Recursable;

public class AttestationTreeBuilderTest {

    @Test
    @DisplayName("sample of entities should be arranged according to hierarchy")
    void testAttestationTree() throws Exception {
        Stream<Recursable> thsEntries = Stream.of(
            Util.loadSampleFile(ThsEntryEntity.class, "E7YEQAEKZVEJ5PX7WKOXY2QEEM"),
            Util.loadSampleFile(ThsEntryEntity.class, "N673TBXEGJDDBO6B6DZXKT64YQ")
        );
        AttestationTreeBuilder treeBuilder = AttestationTreeBuilder.of(thsEntries);
        var roots = treeBuilder.getRoots().collect(Collectors.toList());
        assertAll("entities should be organized according to hierarchic relations",
            () -> assertEquals(1, roots.size(), "only 1 root"),
            () -> assertEquals(
                "N673TBXEGJDDBO6B6DZXKT64YQ",
                ((BaseEntity) roots.get(0).getEntity()).getId(),
                "entity highest up in hierarchy should be root node"
            )
        );
    }

}
