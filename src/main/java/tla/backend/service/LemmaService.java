package tla.backend.service;

import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.domain.command.LemmaSearch;
import tla.domain.command.TypeSpec;
import tla.domain.dto.DocumentDto;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.model.Language;
import tla.domain.model.Passport;
import tla.domain.model.Script;
import tla.domain.model.extern.AttestedTimespan;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Slf4j
@Service
@ModelClass(value = LemmaEntity.class, path = "lemma")
public class LemmaService extends QueryService<LemmaEntity> {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private TextService textService;

    @Autowired
    private ThesaurusService thsService;

    @Autowired
    private ElasticsearchOperations operations;

    public SortedMap<String, Long> countOccurrencesPerText(String lemmaId) {
        return null;
    }

    @Override
    public ElasticsearchRepository<LemmaEntity, String> getRepo() {
        return repo;
    }

    /**
     * Extends superclass implementation {@link QueryService#getDetails(String)}
     * in that lemma attestations are computed from occurrences and put into the
     * wrapped lemma DTO.
     *
     * @see {@link #computeAttestedTimespans(String)}
     */
    @Override
    public SingleDocumentWrapper<DocumentDto> getDetails(String id) {
        LemmaEntity lemma = retrieve(id);
        if (lemma == null) {
            return null;
        }
        SingleDocumentWrapper<DocumentDto> wrapper = super.getDetails(id);
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
        return periodCounts.values();
    }

    public Page<LemmaEntity> search(Query query) {
        log.info("query: {}", query);
        return operations.queryForPage(query, LemmaEntity.class, IndexCoordinates.of("lemma"));
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
        TypeSpec wordClass = command.getPos();
        if (wordClass.getType() != null) {
            if (wordClass.getType().equals("excl_names")) {
                query.mustNot(termQuery("type", "entity_name"));
            } else if (wordClass.getType().equals("any")) {
            } else if (!wordClass.getType().trim().isEmpty()) {
                query.must(termQuery("type", wordClass.getType()));
            }
        }
        if (wordClass.getSubtype() != null && !wordClass.getSubtype().trim().isEmpty()) {
            query.must(termQuery("subtype", wordClass.getSubtype()));
        }
        return query;
    }

    public Query createLemmaSearchQuery(LemmaSearch command, Pageable pageable) {
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
        if (command.getPos() != null) {
            qb.must(wordClassQuery(command));
        }
        return new NativeSearchQueryBuilder()
            .withQuery(qb)
            .withPageable(pageable)
            .build();
    }

}