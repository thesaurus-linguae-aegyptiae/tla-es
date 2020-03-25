package tla.backend.service;

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.io.IOException;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.Indexable;
import tla.backend.es.model.ModelConfig;
import tla.backend.es.model.TLAEntity;

@Slf4j
public abstract class QueryService<T extends Indexable> {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private EntityMapper mapper;


    public SearchResponse query(
        Class<? extends Indexable> entityClass,
        QueryBuilder queryBuilder,
        AggregationBuilder aggsBuilder
    ) {
        String index = ModelConfig.getIndexName(entityClass);
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

    /**
     * look up single entity
     */
    public abstract T retrieve(String id);

    /**
     * Tries to find the ES document identified by eclass and ID.
     */
    public TLAEntity retrieveSingleBTSDoc(String eclass, String id) {
        Class<? extends TLAEntity> modelClass = ModelConfig.getModelClass(eclass);
        QueryBuilder qb = idsQuery().addIds(id);
        SearchResponse res = query(modelClass, qb, null);
        try {
            if (res.getHits().totalHits == 1) {
                SearchHit hit = res.getHits().getAt(0);
                return mapper.mapToObject(
                    hit.getSourceAsString(),
                    modelClass
                );
            }
        } catch (IOException e) {
            log.error(
                String.format(
                    "Could not retrieve %s document with ID %s",
                    modelClass.getName(),
                    id
                ),
                e
            );
        }
        return null;
    }

}