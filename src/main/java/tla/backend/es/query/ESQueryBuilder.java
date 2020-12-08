package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.idsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.domain.model.meta.AbstractBTSBaseClass;

/**
 * This is an abstract search command adapter.
 */
@Slf4j
@Getter
public abstract class ESQueryBuilder implements TLAQueryBuilder {

    /**
     * The desired DTO type in which search results ought to be sent.
     */
    private Class<? extends AbstractBTSBaseClass> dtoClass;

    private BoolQueryBuilder nativeRootQueryBuilder;
    private List<AbstractAggregationBuilder<?>> nativeAggregationBuilders;

    private List<TLAQueryBuilder.QueryDependency<?>> dependencies;

    private SearchHits<?> results;

    public ESQueryBuilder() {
        this.nativeRootQueryBuilder = boolQuery();
        this.nativeAggregationBuilders = new LinkedList<>();
        this.dependencies = new LinkedList<>();
    }

    /**
     * Put together an actual Elasticsearch query ready for execution.
     */
    public NativeSearchQuery buildNativeSearchQuery() {
        var qb = new NativeSearchQueryBuilder().withQuery(
            this.getNativeRootQueryBuilder()
        );
        log.info("query: {}", this.getNativeRootQueryBuilder());
        this.getNativeAggregationBuilders().forEach(
            agg -> {
                log.info("add aggregation to query: {}", agg);
                qb.addAggregation(agg);
            }
        );
        return qb.build();
    }

    public void setResults(SearchHits<?> hits) { // maybe write a TLAQueryResult wrapper instead..
        this.results = hits;
    }

    public void setDTOClass(Class<? extends AbstractBTSBaseClass> dtoClass) {
        log.info("set DTO of search command adapter: {}", dtoClass);
        this.dtoClass = dtoClass;
    }

    public void setId(String[] ids) {
        if (ids != null) {
            log.info("add {} IDs to query", ids.length);
            this.filter(
                idsQuery().addIds(ids)
            );
        }
    }

    public void setEditor(String name) {
        if (name != null) {
            this.must(
                boolQuery().should(
                    matchQuery("editors.author", name)
                ).should(
                    matchQuery("editors.contributors", name)
                )
            );
        }
    }

}