package tla.backend.service.component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.LinkedEntity;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.service.EntityService;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.Resolvable;

public class EntityRetrieval {

    public static class BulkEntityResolver {

        /**
         * object reference target IDs grouped by eclass
         */
        protected Map<String, Set<String>> refs;

        public BulkEntityResolver() {
            this.refs = new HashMap<>();
        }

        public static BulkEntityResolver of(Collection<? extends Resolvable> references) {
            return new BulkEntityResolver().addAll(references);
        }

        /**
         * Take all objectreferences in an entity's <code>relations</code> map and feeds them into
         * a new {@link BulkEntityResolver} instance.
         */
        public static BulkEntityResolver from(LinkedEntity entity) {
            var bulk = new BulkEntityResolver();
            if (entity.getRelations() != null) {
                entity.getRelations().entrySet().forEach(
                    e -> bulk.addAll(e.getValue())
                );
            }
            return bulk;
        }

        /**
         * Add object references to bulk retrieval queue.
         */
        public BulkEntityResolver addAll(Collection<? extends Resolvable> references) {
            if (references != null) {
                references.forEach(
                    ref -> this.add(ref)
                );
            }
            return this;
        }

        /**
         * Merge another bulk retriever's object references queue into this bulk retriever.
         */
        public BulkEntityResolver merge(BulkEntityResolver bulkResolver) {
            bulkResolver.getQueue().entrySet().forEach(
                e -> e.getValue().forEach(
                    v -> this.add(
                        ObjectReference.builder().id(v).eclass(e.getKey()).build()
                    )
                )
            );
            return this;
        }

        /**
         * Get object reference queue, which is a map with <code>eClass</code> values as keys,
         * associated with sets of entity IDs.
         */
        public Map<String, Set<String>> getQueue() {
            return this.refs;
        }

        /**
         * Add a single object reference to bulk retrieval queue.
         */
        protected void add(Resolvable ref) {
            this.refs.merge(
                ref.getEclass(),
                new HashSet<>(List.of(ref.getId())),
                (cur, id) -> {
                    cur.addAll(id);
                    return cur;
                }
            );
        }

        /**
         * Retrieve referenced object from respective ES repositories.
         */
        public Collection<Indexable> resolve() {
            return this.refs.entrySet().stream().flatMap(
                e -> this.resolve(e.getKey(), e.getValue())
            ).collect(
                Collectors.toList()
            );
        }

        /**
         * Retrieve object references to entities of specified type from respective ES repository.
         */
        protected Stream<? extends Indexable> resolve(String eclass, Collection<String> ids) {
            EntityService<?,?,?> service = EntityService.getService(
                ModelConfig.getModelClass(eclass)
            );
            return StreamSupport.stream(
                service.getRepo().findAllById(ids).spliterator(),
                false
            );
        }

    }

}
