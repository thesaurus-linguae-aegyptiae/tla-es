package tla.backend.service;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.OccurrenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.repo.TextRepo;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class TextService extends QueryService<TextEntity> {

    @Autowired
    private TextRepo textRepo;

    @Autowired
    private ThesaurusService thsService;

    @Override
    public ElasticsearchRepository<TextEntity, String> getRepo() {
        return textRepo;
    }

    public Map<String, Long> countOccurrencesPerText(String lemmaId) {
        final String AGG_ID = "aggregation_around_text_id";
        SearchResponse response = query(
            OccurrenceEntity.class,
            termQuery(
                "lemma.id",
                lemmaId
            ),
            AggregationBuilders
                .terms(AGG_ID)
                .field("location.textId")
                .order(BucketOrder.count(false))
                .size(10000000)
        );
        Terms freqPerText = (Terms) response.getAggregations().asMap().get(AGG_ID);
        return freqPerText.getBuckets().stream()
            .collect(
                Collectors.toMap(
                    Bucket::getKeyAsString,
                    Bucket::getDocCount
                )
            );
    }

    /** 
     * Returns first and last year of the time span a text has been attributed to. 
     *
    */
    public int[] getTimespan(String textId) {
        TextEntity text = textRepo.findById(textId).get();
        SortedSet<Integer> years = new TreeSet<>();
        thsService.extractThsEntriesFromPassport(
            text.getPassport(),
            "date.date.date"
        ).stream().forEach(
            term -> {
                years.addAll(
                    term.extractTimespan()
                );
            }
        );
        return new int[] {
            years.first(),
            years.last()
        };
    }

}