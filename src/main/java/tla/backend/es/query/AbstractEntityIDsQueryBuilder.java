package tla.backend.es.query;

import java.util.Collection;
import java.util.List;

import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;

import tla.backend.es.model.meta.Indexable;
import tla.domain.command.SearchCommand;
import tla.domain.dto.meta.AbstractDto;

public abstract class AbstractEntityIDsQueryBuilder<S extends SearchCommand<? extends AbstractDto>, T extends Indexable> 
        extends AbstractEntityQueryBuilder<S, T> implements IDQuery {

    protected AbstractEntityIDsQueryBuilder(S command, Class<T> target) {
        super(command, target);
    }

    /**
     * Adds an aggregation named <code>ids</code> which will return one bucket
     * per matching document.
     */
    @Override
    protected Collection<AbstractAggregationBuilder<?>> getAggregations() {
        return List.of(
            AggregationBuilders.terms("ids")
                .field("id")
                .size(200000)
        );
    }

    /**
     * just returns {@link AbstractEntityQueryBuilder#build(Pageable)} default implementation,
     * but without any paging.
     */
    @Override
    public NativeSearchQuery build() {
        return super.build(Pageable.unpaged());
    }

}
