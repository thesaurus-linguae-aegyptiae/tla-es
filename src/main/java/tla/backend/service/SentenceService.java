package tla.backend.service;

import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.SentenceEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.service.component.EntityRetrieval;
import tla.domain.command.SearchCommand;
import tla.domain.dto.SentenceDto;

@Service
@ModelClass(value = SentenceEntity.class, path = "sentence")
public class SentenceService extends EntityService<SentenceEntity, ElasticsearchRepository<SentenceEntity, String>, SentenceDto> {

    private final static String LEMMA_FREQ_AGG_NAME = "aggr_around_text_id";

    @Autowired
    private SentenceRepo repo;

    @Override
    public ElasticsearchRepository<SentenceEntity, String> getRepo() {
        return repo;
    }

    /**
     * make sure containing text gets included.
     */
    @Override
    protected EntityRetrieval.BulkEntityResolver retrieveRelatedDocs(SentenceEntity document) {
        EntityRetrieval.BulkEntityResolver relatedDocuments = super.retrieveRelatedDocs(document);
        var text = this.retrieveSingleBTSDoc(
            "BTSText", document.getContext().getTextId()
        );
        relatedDocuments.merge(
            super.retrieveReferencedThesaurusEntries(text)
        );
        return relatedDocuments;
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
                .field("context.textId")
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

    @Override
    public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<SentenceDto> command) {
        return SentenceSearchQueryBuilder.class;
    }

}