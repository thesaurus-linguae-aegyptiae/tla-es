package tla.backend.service.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.TLAQueryBuilder;
import tla.backend.es.query.TLAQueryBuilder.QueryDependency;
import tla.domain.dto.extern.PageInfo;

@Slf4j
@Service
public class SearchService {

    public class QueryExecutor {
        private ESQueryBuilder query;
        public QueryExecutor(ESQueryBuilder query) {
            this.query = query;
        }
        public SearchHits<?> run() {
            log.info("run query");
            log.info("dependency: {}", this.query.getDependencies());
            List<QueryDependency<?>> queue = this.query.resolveDependencies();
            log.info("dependencies: {}", queue.size());
            for (QueryDependency<?> dependency : queue) {
                log.info("execute query dependency {}", dependency);
                dependency.getQuery().setResults(
                    executeSearchQuery(
                        ((ESQueryBuilder) dependency.getQuery()).buildNativeSearchQuery(),
                        dependency.getQuery().getModelClass()
                    )
                );
                dependency.resolve();
            }
            log.info("run head query");
            return executeSearchQuery(
                this.query.buildNativeSearchQuery(),
                this.query.getModelClass()
            );
        }
    }

    /**
     * How many search results fit in 1 page.
     */
    public static final int SEARCH_RESULT_PAGE_SIZE = 20;

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    protected RestHighLevelClient restClient;

    /**
     * Execute native Elasticsearch query against a single index.
     */
    public SearchHits<?> executeSearchQuery(
        NativeSearchQuery query, Class<? extends Indexable> modelClass
    ) {
        return operations.search(
            query,
            modelClass,
            operations.getIndexCoordinatesFor(modelClass)
        );
    }

    public SearchHits<?> register(ESQueryBuilder query) {
        return new QueryExecutor(query).run();
    }

    /**
     * Creates and executes native query from an Elasticsearch query builder against whatever index
     * is known for storing documents of the type specified via the <code>entityClass</code> parameter.
     */
    public SearchResponse query(
        Class<? extends Indexable> entityClass,
        QueryBuilder queryBuilder,
        AggregationBuilder aggsBuilder
    ) {
        String index = operations.getIndexCoordinatesFor(entityClass).getIndexName();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .query(queryBuilder);
        if (aggsBuilder != null) {
            searchSourceBuilder = searchSourceBuilder.aggregation(aggsBuilder);
        }
        SearchRequest request = new SearchRequest()
            .indices(
                index
            )
            .source(
                searchSourceBuilder
            );
        SearchResponse response = null;
        try {
            response = restClient
                .search(
                    request,
                    RequestOptions.DEFAULT
                );
        } catch (Exception e) {
            log.error(
                String.format(
                    "could not query index %s",
                    index
                ),
                e
            );
        }
        return response;
    }

    /**
     * extract a simple map of terms and their counts from
     * a bucketed aggregation's buckets.
     */
    private Map<String, Long> getFacetsFromBuckets(Terms agg) {
        return ((Terms) agg).getBuckets().stream().collect(
            Collectors.toMap(
                Terms.Bucket::getKeyAsString,
                Terms.Bucket::getDocCount
            )
        );
    }

    /**
     * Converts terms aggregations in an Elasticsearch search response to a map of field
     * value counts.
     */
    public Map<String, Map<String, Long>> extractFacets(SearchHits<?> hits) {
        if (hits.hasAggregations()) {
            Map<String, Map<String, Long>> facets = new HashMap<>();
            for (Aggregation agg : hits.getAggregations().asList()) {
                if (agg instanceof Terms) {
                    facets.put(
                        agg.getName(),
                        getFacetsFromBuckets((Terms) agg)
                    );
                } else if (agg instanceof Filter) {
                    ((Filter) agg).getAggregations().asList().stream().filter(
                        sub -> sub instanceof Terms
                    ).forEach(
                        sub -> facets.put(
                            agg.getName(),
                            getFacetsFromBuckets((Terms) sub)
                        )
                    );
                }
            }
            return facets;
        } else {
            return null;
        }
    }

    /**
     * Create a serializable transfer object containing page information
     * about an ES search result.
     */
    public PageInfo pageInfo(SearchHits<?> hits, Pageable pageable) {
        return PageInfo.builder()
            .number(pageable.getPageNumber())
            .totalElements(hits.getTotalHits())
            .size(SEARCH_RESULT_PAGE_SIZE)
            .numberOfElements(
                Math.min(
                    SEARCH_RESULT_PAGE_SIZE,
                    hits.getSearchHits().size()
                )
            ).totalPages(
                (int) hits.getTotalHits() / SEARCH_RESULT_PAGE_SIZE + 1 // TODO
            ).build();
    }

}