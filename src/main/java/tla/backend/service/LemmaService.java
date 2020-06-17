package tla.backend.service;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.query.AbstractEntityQueryBuilder;
import tla.backend.es.query.EntityQueryBuilders;
import tla.backend.es.repo.LemmaRepo;
import tla.domain.command.LemmaSearch;
import tla.domain.command.SearchCommand;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.Passport;
import tla.domain.model.extern.AttestedTimespan;

@Slf4j
@Service
@ModelClass(value = LemmaEntity.class, path = "lemma")
public class LemmaService extends EntityService<LemmaEntity, ElasticsearchRepository<LemmaEntity, String>, LemmaDto> {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private TextService textService;

    @Autowired
    private SentenceService sentenceService;

    @Autowired
    private ThesaurusService thsService;

    @Override
    public ElasticsearchRepository<LemmaEntity, String> getRepo() {
        return repo;
    }

    /**
     * Extends superclass implementation {@link EntityService#getDetails(String)} in
     * that lemma attestations are computed from occurrences and put into the
     * wrapped lemma DTO.
     *
     * @see {@link #computeAttestedTimespans(String)}
     */
    @Override
    public SingleDocumentWrapper<? extends AbstractDto> getDetails(String id) {
        LemmaEntity lemma = retrieve(id);
        if (lemma == null) {
            return null;
        }
        SingleDocumentWrapper<? extends AbstractDto> wrapper = super.getDetails(id);
        ((LemmaDto) wrapper.getDoc()).setAttestations(
            new LinkedList<>(
                this.computeAttestedTimespans(id)
            )
        );
        return wrapper;
    }

    /**
     * collects all thesaurus terms representing a time period and being referenced
     * in texts containing the specified lemma, and counts the number of texts and
     * total occurrences for each one.
     */
    public Collection<AttestedTimespan> computeAttestedTimespans(String lemmaId) {
        Map<String, Long> freqPerText = sentenceService.lemmaFrequencyPerText(lemmaId);
        Map<String, AttestedTimespan> periodCounts = new HashMap<>();
        for (Entry<String, Long> e : freqPerText.entrySet()) {
            TextEntity t = textService.retrieve(e.getKey());
            Passport p = t.getPassport();
            List<ThsEntryEntity> dateTerms = thsService.extractThsEntriesFromPassport(
                p, "date.date.date"
            );
            if (dateTerms.size() > 0) {
                if (dateTerms.size() != 1) {
                    log.error("text {} has not exactly 1 date term assigned to it (rather {})", e.getKey(), dateTerms.size());
                } else {
                    ThsEntryEntity term = dateTerms.get(0);
                    if (periodCounts.containsKey(term.getId())) {
                        AttestedTimespan timespan = periodCounts.get(term.getId());
                        timespan.getAttestations().add(
                            AttestedTimespan.AttestationStats.builder()
                                .count(e.getValue())
                                .texts(1)
                                .build()
                        );
                    } else {
                        AttestedTimespan timespan = AttestedTimespan.builder()
                            .attestations(
                                AttestedTimespan.AttestationStats.builder()
                                    .count(e.getValue())
                                    .texts(1)
                                    .build()
                            ).period(term.toAttestedPeriod())
                            .build();
                        periodCounts.put(term.getId(), timespan);
                    }
                }
            }
        }
        return periodCounts.values();
    }

    public Map<String, Long> getMostFrequent(int limit) {
        SearchResponse response = query(SentenceEntity.class, matchAllQuery(),
                AggregationBuilders.nested("aggs", "tokens").subAggregation(AggregationBuilders.terms("lemmata")
                        .field("tokens.lemma.id").order(BucketOrder.count(false)).size(limit)));
        Nested aggs = response.getAggregations().get("aggs");
        Terms terms = aggs.getAggregations().get("lemmata");
        return terms.getBuckets().stream()
                .collect(Collectors.toMap(Terms.Bucket::getKeyAsString, Terms.Bucket::getDocCount));
    }

    @Override
    protected AbstractEntityQueryBuilder<?, ?> getEntityQueryBuilder(SearchCommand<?> search) {
        if (search.getDTOClass().equals(LemmaDto.class)) {
            if (search instanceof LemmaSearch) {
                return EntityQueryBuilders.lemmaEntityQuery((LemmaSearch) search);
            }
        }
        return null;
    }

}
