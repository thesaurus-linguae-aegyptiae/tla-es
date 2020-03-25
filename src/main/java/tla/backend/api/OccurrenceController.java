package tla.backend.api;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.ModelConfig;
import tla.backend.es.model.OccurrenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.OccurrenceRepo;
import tla.backend.service.TextService;
import tla.backend.service.ThesaurusService;
import tla.domain.model.Passport;
import tla.domain.model.extern.AttestedTimespan;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Slf4j
@RestController
@RequestMapping("/occ")
public class OccurrenceController {

    @Autowired
    private OccurrenceRepo repo;

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private TextService textService;

    @Autowired
    private ThesaurusService thsService;

    @RequestMapping(method = RequestMethod.GET, value = "/lemma/{id}")
    public ResponseEntity<Map<String, Long>> lemmaOccurrences(@PathVariable String id) {
        SearchQuery query = new NativeSearchQueryBuilder().withQuery(termQuery("lemma.id", id)).build();
        long occurrenceCount = restTemplate.count(query, OccurrenceEntity.class);
        return new ResponseEntity<Map<String, Long>>(Map.of(id, occurrenceCount), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/texts/lemma/{id}")
    public ResponseEntity<Collection<AttestedTimespan>> texts(@PathVariable(name = "id") String lemmaId) {
        Map<String, Long> freqPerText = textService.countOccurrencesPerText(lemmaId);
        Map<String, AttestedTimespan> periodCounts = new HashMap<>();
        for (Entry<String, Long> e : freqPerText.entrySet()) {
            TextEntity t = textService.retrieve(e.getKey());
            Passport p = t.getPassport();
            List<ThsEntryEntity> dateTerms = thsService.extractThsEntriesFromPassport(
                p,
                "date.date.date"
            );
            if (dateTerms.size() > 0) {
                if (dateTerms.size() != 1) {
                    log.error("text {} has not exactly 1 date term assigned to it", e.getKey());
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
                            )
                            .period(
                                term.toAttestedPeriod()
                            )
                            .build();
                        periodCounts.put(
                            term.getId(),
                            timespan
                        );
                    }
                }
            }
        }
        return new ResponseEntity<Collection<AttestedTimespan>>(
            periodCounts.values(),
            HttpStatus.OK
        );
    }


    @RequestMapping(method = RequestMethod.GET, value = "/lemmata")
    public ResponseEntity<Map<String, Long>> frequencies() throws IOException {
        TermsAggregationBuilder aggs = AggregationBuilders
            .terms("lemma_frequency")
            .field("lemma.id")
            .order(BucketOrder.count(false))
            .size(100000);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .aggregation(aggs);
        SearchRequest request = new SearchRequest()
            .indices(
                ModelConfig.getIndexName(OccurrenceEntity.class)
            )
            .source(searchSourceBuilder)
            .requestCache(true);
        SearchResponse response = restTemplate.getClient().search(
            request,
            RequestOptions.DEFAULT
        );
        Aggregations lemmaFrequencies = response.getAggregations();

        Terms top = (Terms) lemmaFrequencies.asMap().get("lemma_frequency");
        Map<String, Long> data = top.getBuckets().stream()
            .collect(
                Collectors.toMap(
                    Bucket::getKeyAsString,
                    Bucket::getDocCount
                )
            );

        return new ResponseEntity<Map<String, Long>>(
            data,
            HttpStatus.OK
        );
    }
    
    @RequestMapping(method = RequestMethod.GET, value = "/count")
    public ResponseEntity<Long> count() {
        log.debug("counting occurrences: {}", repo.count());
        return new ResponseEntity<Long>(
            repo.count(),
            HttpStatus.OK
        );
    }

    @RequestMapping(method = RequestMethod.POST, value = "/batch")
    public ResponseEntity<Iterable<OccurrenceEntity>> postLemma(@RequestBody Iterable<OccurrenceEntity> lemmata) {
        return new ResponseEntity<Iterable<OccurrenceEntity>>(
            repo.saveAll(lemmata),
            HttpStatus.CREATED
        );
    }

}