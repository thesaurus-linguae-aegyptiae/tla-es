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

import tla.backend.es.model.meta.ModelConfig;
import tla.backend.service.EntityService;
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

        public BulkEntityResolver addAll(Collection<? extends Resolvable> references) {
            references.forEach(
                ref -> this.add(ref)
            );
            return this;
        }

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

        public List<?> resolve() {
            return this.refs.entrySet().stream().flatMap(
                e -> this.resolve(e.getKey(), e.getValue())
            ).collect(
                Collectors.toList()
            );
        }

        protected Stream<?> resolve(String eclass, Collection<String> ids) {
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
