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
import tla.backend.es.query.ESQueryResult;
import tla.backend.es.query.TLAQueryBuilder.QueryDependency;

@Slf4j
@Service
public class SearchService {

    /**
     * Execute search command query adapter and its dependencies.
     */
    public class QueryExecutor {

        private ESQueryBuilder query;

        public QueryExecutor(ESQueryBuilder query) {
            this.query = query;
        }

        public ESQueryResult<?> run(Pageable page) {
            log.info("run query for page {}", page);
            log.info("dependency: {}", this.query.getDependencies());
            List<QueryDependency<?>> queue = this.query.resolveDependencies();
            log.info("dependencies: {}", queue.size());
            for (QueryDependency<?> dependency : queue) {
                log.info("execute query dependency {}", dependency);
                dependency.getQuery().setResult(
                    executeSearchQuery(
                        ((ESQueryBuilder) dependency.getQuery()).buildNativeSearchQuery(
                            Pageable.unpaged() // TODO size=0
                        ),
                        dependency.getQuery().getModelClass()
                    )
                );
                dependency.resolve();
            }
            log.info("run head query");
            return executeSearchQuery(
                this.query.buildNativeSearchQuery(page),
                this.query.getModelClass()
            );
        }

    }

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    protected RestHighLevelClient restClient;

    /**
     * Creates a new {@link QueryExecutor} for a given query.
     *
     * TODO: actual registry?
     */
    public QueryExecutor register(ESQueryBuilder query) {
        return new QueryExecutor(query);
    }

    /**
     * Execute native Elasticsearch query against a single index.
     */
    public <T extends Indexable> ESQueryResult<T> executeSearchQuery(
        NativeSearchQuery query, Class<T> modelClass
    ) {
        var page = query.getPageable();
        log.info("query paged: {}", page.isPaged());
        return new ESQueryResult<T>(
            operations.<T>search(
                query,
                modelClass,
                operations.getIndexCoordinatesFor(modelClass)
            ),
            page
        );
    }

    /**
     * count exact number of search results for given query (we don't really need this).
     */
    public <T extends Indexable> long countSearchResults(NativeSearchQuery query, Class<T> modelClass) {
        return query.getPageable().isUnpaged() ? 0L : operations.count(
            query, modelClass, operations.getIndexCoordinatesFor(modelClass)
        );
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


}