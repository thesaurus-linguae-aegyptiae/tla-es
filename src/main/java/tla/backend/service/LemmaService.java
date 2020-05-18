package tla.backend.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.domain.dto.DocumentDto;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.model.Passport;
import tla.domain.model.extern.AttestedTimespan;

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

}