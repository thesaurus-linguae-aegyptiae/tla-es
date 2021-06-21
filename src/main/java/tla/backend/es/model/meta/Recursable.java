package tla.backend.es.model.meta;

import tla.backend.es.model.parts.ObjectPath;
import tla.domain.model.meta.Resolvable;

public interface Recursable {

    /**
     * get paths whose traversal through the object tree lead to this entity.
     */
    public ObjectPath[] getPaths();

    public void setPaths(ObjectPath[] paths);

    /**
     * determine whether another object is living in this entity's sub tree.
     */
    default public boolean isAncestorOf(Recursable entity) {
        if (entity.getPaths() != null) {
            Resolvable ref = ((BaseEntity) this).toDTOReference();
            for (ObjectPath path : entity.getPaths()) {
                if (path.stream().anyMatch(
                    segment -> segment.getId().equals(ref.getId())
                )) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * determine whether another object is an ancestral node of this entity.
     */
    default public boolean hasAncestor(Recursable entity) {
        return entity.isAncestorOf(this);
    }

}
