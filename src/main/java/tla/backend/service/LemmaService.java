package tla.backend.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.domain.command.LemmaSearch;
import tla.domain.command.SearchCommand;
import tla.domain.command.TypeSpec;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.Language;
import tla.domain.model.Passport;
import tla.domain.model.Script;
import tla.domain.model.extern.AttestedTimespan;

@Slf4j
@Service
@ModelClass(value = LemmaEntity.class, path = "lemma")
public class LemmaService extends EntityService<LemmaEntity, LemmaDto> {

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
     * Extends superclass implementation {@link EntityService#getDetails(String)}
     * in that lemma attestations are computed from occurrences and put into the
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
     * collects all thesaurus terms representing a time period and being referenced in
     * texts containing the specified lemma, and counts the number of texts and
     * total occurrences for each one.
     */
    public Collection<AttestedTimespan> computeAttestedTimespans(String lemmaId) {
        Map<String, Long> freqPerText = sentenceService.lemmaFrequencyPerText(lemmaId);
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
        return periodCounts.values();
    }

    public Map<String, Long> getMostFrequent(int limit) {
        SearchResponse response = query(
            SentenceEntity.class,
            matchAllQuery(),
            AggregationBuilders.nested(
                "aggs",
                "tokens"
            ).subAggregation(
                AggregationBuilders.terms("lemmata").field(
                    "tokens.lemma.id"
                ).order(
                    BucketOrder.count(false)
                ).size(limit)
            )
        );
        Nested aggs = response.getAggregations().get("aggs");
        Terms terms = aggs.getAggregations().get("lemmata");
        return terms.getBuckets().stream().collect(
            Collectors.toMap(
                Terms.Bucket::getKeyAsString,
                Terms.Bucket::getDocCount
            )
        );
    }

    private BoolQueryBuilder scriptFilter(LemmaSearch command) {
        BoolQueryBuilder scriptFilter = boolQuery();
        List<Script> scripts = Arrays.asList(command.getScript());
        if (!scripts.contains(Script.HIERATIC)) {
            if (scripts.contains(Script.DEMOTIC)) {
                scriptFilter.must(prefixQuery("id", "d"));
            }
        } else {
            if (!scripts.contains(Script.DEMOTIC)) {
                scriptFilter.mustNot(prefixQuery("id", "d"));
            }
        }
        return scriptFilter;
    }

    private BoolQueryBuilder translationQuery(LemmaSearch command) {
        BoolQueryBuilder translationsQuery = boolQuery();
        if (command.getTranslation().getLang() != null && command.getTranslation().getLang().length > 0) {
            for (Language lang : command.getTranslation().getLang()) {
                translationsQuery.should(
                    matchQuery(
                        String.format("translations.%s", lang),
                        command.getTranslation().getText()
                    )
                );
            }
        }
        return translationsQuery;
    }

    private BoolQueryBuilder wordClassQuery(LemmaSearch command) {
        BoolQueryBuilder query = boolQuery();
        TypeSpec wordClass = command.getWordClass();
        if (wordClass.getType() != null) {
            if (wordClass.getType().equals("excl_names")) {
                query.mustNot(termQuery("type", "entity_name"));
            } else if (wordClass.getType().equals("any")) {
            } else if (!wordClass.getType().isBlank()) {
                query.must(termQuery("type", wordClass.getType()));
            }
        }
        if (wordClass.getSubtype() != null && !wordClass.getSubtype().isBlank()) {
            query.must(termQuery("subtype", wordClass.getSubtype()));
        }
        return query;
    }

    /**
     * Make sure query string is lowercased in query.
     */
    private static QueryBuilder termQuery(String field, String value) {
        if (value == null || value.isBlank()) {
            return boolQuery();
        } else {
            return org.elasticsearch.index.query.QueryBuilders.termQuery(
                field,
                value.toLowerCase()
            );
        }
    }

    /**
     * Create query requiring lemma to have annotations.
     */
    private BoolQueryBuilder annotationTypeQuery(LemmaSearch command) {
        BoolQueryBuilder q = boolQuery();
        TypeSpec anno = command.getAnnotationType();
        if (anno.getType() != null) {
            if (!anno.getType().isBlank()) {
                q.must(
                    termQuery("relations.contains.eclass", "BTSAnnotation")
                );
            }
        }
        return q;
    }

    /**
     * Representation of search order specifications.
     */
    protected static class SortSpec {

        public static final String DELIMITER = "_";

        protected String field;
        protected SortOrder order;

        public SortSpec(String field) {
            this(field, SortOrder.ASC);
        }

        public SortSpec(String field, SortOrder order) {
            this.field = field;
            this.order = order;
        }

        public SortSpec(String field, String order) {
            this(
                field,
                order.toLowerCase().equals("desc") ? SortOrder.DESC : SortOrder.ASC
            );
        }

        public static SortSpec from(SearchCommand<LemmaDto> command) {
            return from(command.getSort());
        }

        public static SortSpec from(String source) {
            if (source != null) {
                String[] segm = source.split(DELIMITER);
                String field = String.join(
                    DELIMITER,
                    Arrays.asList(segm).subList(0, segm.length - 1)
                );
                if (segm.length > 1) {
                    return new SortSpec(field, segm[segm.length - 1]);
                } else {
                    return new SortSpec(segm[0]);
                }
            } else {
                return new SortSpec("id");
            }
        }

        public SortBuilder<?> primary() {
            return SortBuilders.fieldSort(this.field).order(this.order);
        }

        public SortBuilder<?> secondary() {
            return SortBuilders.fieldSort("id").order(this.order);
        }
    }

    /**
     * Based on the search command and what fields in there are specified, generate
     * terms aggregations for faceted search.
     */
    protected static List<AbstractAggregationBuilder<?>> lemmaSearchAggregations(LemmaSearch command) {
        List<AbstractAggregationBuilder<?>> aggs = new ArrayList<>();
        TypeSpec wc = command.getWordClass();
        if (wc != null) {
            if (wc.getType() != null && !wc.getType().isBlank()) {
                if (wc.getSubtype() == null || wc.getSubtype().isBlank()) {
                    aggs.add(
                        AggregationBuilders.terms("wordClass.subtype").field("subtype")
                    );
                }
            } else {
                aggs.add(
                    AggregationBuilders.terms("wordClass.type").field("type")
                );
            }
        } else {
            aggs.add(
                AggregationBuilders.terms("worClass.type").field("type")
            );
        }
        if (command.getScript() == null || command.getScript().length < 1) {
            aggs.add(
                AggregationBuilders.terms("script").script(
                    new org.elasticsearch.script.Script(
                        "if (doc['id'].value.startsWith('d')) {return 'demotic';} if (!doc['type'].value.equals('root')) {return 'hieratic';}"
                    )
                )
            );
        }
        TypeSpec anno = command.getAnnotationType();
        if (anno == null || anno.getType() == null || anno.getType().isBlank()) {
            aggs.add(
                AggregationBuilders.filter(
                    "annotationType.type",
                    termQuery("relations.contains.eclass", "BTSAnnotation")
                ).subAggregation(
                    AggregationBuilders.terms("subagg").script(
                        new org.elasticsearch.script.Script(
                            "return 'Lemma';"
                        )
                    )
                )
            );
        }
        log.info("add aggregations to query: {}", aggs.size());
        return aggs;
    }


    /**
     * Convert a search command transfer object into a native Elasticsearch query.
     */
    public Query createLemmaSearchQuery(LemmaSearch command, Pageable pageable) {
        log.info("SEARCH COMMAND: {}", tla.domain.util.IO.json(command));
        BoolQueryBuilder qb = boolQuery();
        if (command.getTranscription() != null) {
           qb.must(matchQuery("name", command.getTranscription()).operator(Operator.AND));
        }
        if (command.getTranslation() != null) {
            qb.must(translationQuery(command));
        }
        if (command.getScript() != null && command.getScript().length > 0) {
            qb.filter(scriptFilter(command));
        }
        if (command.getBibliography() != null && !command.getBibliography().trim().isEmpty()) {
            qb.must(
                matchQuery("passport.bibliography.bibliographical_text_field", command.getBibliography()).operator(Operator.AND)
            );
        }
        if (command.getRoot() != null && !command.getRoot().trim().isEmpty()) {
            qb.must(
                matchQuery("relations.root.name", command.getRoot())
            );
        }
        if (command.getWordClass() != null) {
            qb.must(wordClassQuery(command));
        }
        if (command.getAnnotationType() != null) {
            qb.must(annotationTypeQuery(command));
        }
        NativeSearchQuery lemmaQuery = new NativeSearchQueryBuilder()
            .withQuery(qb)
            .withPageable(pageable)
            .withSort(SortSpec.from(command).primary())
            .withSort(SortSpec.from(command).secondary())
            .build();
        lemmaSearchAggregations(command).forEach(
            agg -> lemmaQuery.addAggregation(agg)
        );
        return lemmaQuery;
    }

}
