package tla.backend.service;

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.io.IOException;
import java.util.Optional;

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
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.BaseEntity;
import tla.backend.es.model.Indexable;
import tla.backend.es.model.ModelConfig;

@Slf4j
public abstract class QueryService<T extends Indexable> {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private EntityMapper mapper;

    /**
     * Subclasses must implement this method and return their respective entity repository
     * (which they presumably got autowired directly into them via spring dependency injection).
     */
    public abstract ElasticsearchRepository<T, String> getRepo();

        /**
     * look up single entity. should return null if it could not be found
     */
    public T retrieve(String id) {
        Optional<T> result = getRepo().findById(id);
        return result.isPresent() ? result.get() : null;
    }

    /**
     * Execute an Elasticsearch query against the index defined for <code>entityClass</code>.
     */
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
     * Tries to find the ES document identified by eclass and ID.
     */
    public BaseEntity retrieveSingleBTSDoc(String eclass, String id) {
        Class<? extends BaseEntity> modelClass = ModelConfig.getModelClass(eclass);
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