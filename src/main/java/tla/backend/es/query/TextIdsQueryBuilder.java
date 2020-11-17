package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilder;

import tla.backend.es.model.TextEntity;
import tla.domain.command.OccurrenceSearch;

public class TextIdsQueryBuilder
        extends AbstractEntityIDsQueryBuilder<OccurrenceSearch, TextEntity> {


    protected TextIdsQueryBuilder(OccurrenceSearch command) {
        super(command, TextEntity.class);
    }

    @Override
    protected QueryBuilder getQuery() {
        // TODO Auto-generated method stub
        return null;
    }

}