package tla.backend.es.model.meta;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import lombok.Getter;
import lombok.Setter;
import tla.backend.es.model.parts.ObjectPath;
import tla.backend.es.model.parts.ObjectReference;
import tla.domain.model.meta.BTSeClass;


public class TreeTest {

    @Getter
    @Setter
    @BTSeClass("eclass")
    static class TreeTestEntity extends BaseEntity implements Recursable {
        ObjectPath[] paths;
        static TreeTestEntity of(String id, String name, String type) {
            var entity = new TreeTestEntity();
            entity.setId(id);
            entity.setType(type);
            entity.setName(name);
            return entity;
        }
    }


    @Test
    void hierarchicEntities() {
        var parent = TreeTestEntity.of("1", "parent", "type");
        var child = TreeTestEntity.of("2", "child", "type");
        child.setPaths(
            new ObjectPath[]{
                ObjectPath.of(
                    ObjectReference.builder().id(
                        parent.getId()
                    ).name(
                        parent.getName()
                    ).type(
                        parent.getType()
                    ).eclass(
                        parent.getEclass()
                    ).build()
                )
            }
        );
        assertTrue(parent.isAncestorOf(child));
        assertTrue(child.hasAncestor(parent));
        assertFalse(child.isAncestorOf(parent));
    }
}
