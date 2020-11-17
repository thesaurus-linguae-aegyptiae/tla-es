package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import tla.backend.es.model.meta.Indexable;
import tla.domain.command.SearchCommand;
import tla.domain.dto.meta.AbstractDto;

public abstract class AbstractEntityQueryBuilder<S extends SearchCommand<? extends AbstractDto>, T extends Indexable> {

    /**
     * search criteria DTO on whose basis a native search query is to be built.
     */
    private S command;

    private Class<T> target;

    /**
     * To be implemented by subclasses. This returns what is going to become the
     * <code>"query"</code> part of the ES search API request body getting assembled
     * in {@link #build(Pageable)}.
     */
    protected abstract QueryBuilder getQuery();

    /**
     * First parameter takes the {@link SearchCommand} instance around which a native
     * search query is to be built. Second parameter specifies an entity type indicating
     * what kind of entities we want to get out of this.
     */
    protected AbstractEntityQueryBuilder(S command, Class<T> targetEntityClass) {
        this.command = command;
        this.target = targetEntityClass;
    }

    /**
     * build query for given search results page.
     */
    public NativeSearchQuery build(Pageable pageable) {
        NativeSearchQuery body = new NativeSearchQueryBuilder()
            .withQuery(this.getQuery())
            .withSort(this.getSortSpec().primary())
            .withPageable(pageable)
            .build();
        this.getAggregations().forEach(
            agg -> body.addAggregation(agg)
        );
        return body;
    }

    /**
     * What kind of entity we want to get out of this.
     * @return {@link Indexable} instance of an entity class
     */
    public Class<T> targetEntityClass() {
        return target;
    }

    /**
     * Supposed to be overwritten by subclasses. Return at least
     * <pre>{@code new SortSpec("id");}</pre>
     */
    protected SortSpec getSortSpec() {
        return new SortSpec("id");
    }

    /**
     * Based on the search command and what fields in there are specified, generate
     * terms aggregations for faceted search.
     */
    protected Collection<AbstractAggregationBuilder<?>> getAggregations() {
        return Collections.emptyList();
    }

    /**
     * return the command used to configure this query builder
     */
    public S getCommand() {
        return this.command;
    }

    /**
     * Make sure query string is lowercased in query.
     */
    public static QueryBuilder termQuery(String field, String value) {
        if (value == null || value.isBlank()) {
            return boolQuery();
        } else {
            return org.elasticsearch.index.query.QueryBuilders.termQuery(
                field,
                value.toLowerCase()
            );
        }
    }

    /**
     * Representation of search order specifications.
     */
    protected static class SortSpec {

        public static final String DELIMITER = "_";

        protected String field;
        protected SortOrder order;

        public SortSpec(String field) {
            this(field, SortOrder.ASC);
        }

        public SortSpec(String field, SortOrder order) {
            this.field = field;
            this.order = order;
        }

        public SortSpec(String field, String order) {
            this(
                field,
                order.toLowerCase().equals("desc") ? SortOrder.DESC : SortOrder.ASC
            );
        }

        public static SortSpec from(SearchCommand<?> command) {
            return from(command.getSort());
        }

        public static SortSpec from(String source) {
            if (source != null) {
                String[] segm = source.split(DELIMITER);
                String field = String.join(
                    DELIMITER,
                    Arrays.asList(segm).subList(0, segm.length - 1)
                );
                if (segm.length > 1) {
                    return new SortSpec(field, segm[segm.length - 1]);
                } else {
                    return new SortSpec(segm[0]);
                }
            } else {
                return new SortSpec("id");
            }
        }

        public SortBuilder<?> primary() {
            return SortBuilders.fieldSort(this.field).order(this.order);
        }

        public SortBuilder<?> secondary() {
            return SortBuilders.fieldSort("id").order(this.order);
        }
    }

}