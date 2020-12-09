package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilder;

import tla.backend.es.model.TextEntity;
import tla.domain.command.SentenceSearch;

public class TextIdsQueryBuilder
        extends AbstractEntityIDsQueryBuilder<SentenceSearch, TextEntity> {


    protected TextIdsQueryBuilder(SentenceSearch command) {
        super(command, TextEntity.class);
    }

    @Override
    protected QueryBuilder getQuery() {
        // TODO Auto-generated method stub
        return null;
    }

}