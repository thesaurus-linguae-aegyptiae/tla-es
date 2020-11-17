package tla.backend.es.query;

import tla.backend.es.model.LemmaEntity;
import tla.domain.command.LemmaSearch;

public class EntityQueryBuilders {

    public static SimpleLemmaEntityQueryBuilder lemmaEntityQuery(LemmaSearch command) {
        return new SimpleLemmaEntityQueryBuilder(command, LemmaEntity.class);
    }

}