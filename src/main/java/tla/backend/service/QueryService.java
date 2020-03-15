package tla.backend.service;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.Indexable;
import tla.backend.es.model.ModelConfig;

@Slf4j
public abstract class QueryService {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    public SearchResponse query(
        Class<? extends Indexable> entityClass,
        QueryBuilder queryBuilder,
        AggregationBuilder aggsBuilder
    ) {
        String index = ModelConfig.getIndexName(entityClass);
        SearchRequest request = new SearchRequest()
            .indices(
                index
            )
            .source(
                new SearchSourceBuilder()
                    .query(queryBuilder)
                    .aggregation(aggsBuilder)
            );
        SearchResponse response = null;
        try {
            response = restTemplate.getClient()
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

}