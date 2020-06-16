package tla.backend.service.search;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;

import tla.backend.es.model.meta.Indexable;
import tla.domain.command.SearchCommand;
import tla.domain.dto.meta.AbstractDto;

public abstract class SearchQueryThingy<S extends SearchCommand<AbstractDto>, T extends Indexable> {

    protected abstract SortBuilder<?> getSortBuilder(S command);

    protected abstract BoolQueryBuilder getQueryBuilder(S command);

    public Query getQuery(S command, Pageable pageable) {
        return new NativeSearchQueryBuilder()
            .withQuery(
                getQueryBuilder(command)
            )
            .withPageable(pageable)
            .withSort(
                getSortBuilder(command)
            )
            .build();
    }

}