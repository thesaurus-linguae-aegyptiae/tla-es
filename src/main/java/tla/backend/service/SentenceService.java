package tla.backend.service;

import org.elasticsearch.action.search.SearchResponse;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.SentenceEntity;
import tla.backend.es.repo.SentenceRepo;

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@ModelClass(value = SentenceEntity.class, path = "sentence")
public class SentenceService extends QueryService<SentenceEntity> {

    private final static String LEMMA_FREQ_AGG_NAME = "aggr_around_text_id";

    @Autowired
    private SentenceRepo repo;

    @Override
    public ElasticsearchRepository<SentenceEntity, String> getRepo() {
        return repo;
    }

    /**
     * Count occurrences of the specified lemma in each text.
     */
    public Map<String, Long> lemmaFrequencyPerText(String lemmaId) {
        SearchResponse response = query(
            SentenceEntity.class,
            nestedQuery(
                "tokens",
                termQuery(
                    "tokens.lemma.id", lemmaId
                ),
                ScoreMode.None
            ),
            AggregationBuilders.terms(LEMMA_FREQ_AGG_NAME)
                .field("textId")
                .order(BucketOrder.count(false))
                .size(10000000)
        );
        Terms frequencies = (Terms) response.getAggregations().asMap().get(LEMMA_FREQ_AGG_NAME);
        return frequencies.getBuckets().stream().collect(
            Collectors.toMap(
                Terms.Bucket::getKeyAsString,
                Terms.Bucket::getDocCount
            )
        );
    }

}