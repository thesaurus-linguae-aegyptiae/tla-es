package tla.backend.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.ModelConfig;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.model.ObjectReference;
import tla.domain.model.Passport;
import tla.domain.model.extern.AttestedTimespan;

@Slf4j
@Service
public class LemmaService extends QueryService<LemmaEntity> {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private TextService textService;

    @Autowired
    private ThesaurusService thsService;

    @Autowired
    private AnnotationService annotationService;


    public SortedMap<String, Long> countOccurrencesPerText(String lemmaId) {
        return null;
    }

    @Override
    public LemmaEntity retrieve(String id) {
        Optional<LemmaEntity> result = repo.findById(id);
        if (result.isPresent()) {
            return result.get();
        } else {
            return null;
        }
    }

    /**
     * returns null if lookup fails
     */
    public SingleDocumentWrapper<LemmaDto> getLemmaDetails(String id) {
        LemmaEntity lemma = retrieve(id);
        if (lemma == null) {
            return null;
        }
        SingleDocumentWrapper<LemmaDto> wrapper = new SingleDocumentWrapper<>(
                (LemmaDto) lemma.toDTO()
        );
        wrapper.getDoc().setAttestations(
            new LinkedList<>(
                this.computeAttestedTimespans(id)
            )
        );
        for (Entry<String, List<ObjectReference>> e : lemma.getRelations().entrySet()) {
            List<ObjectReference> objectReferences = e.getValue();
            for (ObjectReference ref : objectReferences) {
                if (ref.getEclass().equals(ModelConfig.getEclass(LemmaEntity.class))) {
                    LemmaEntity relatedLemma = retrieve(
                        ref.getId()
                    );
                    wrapper.addRelated(relatedLemma.toDTO());
                } else if (ref.getEclass().equals(ModelConfig.getEclass(AnnotationEntity.class))) {
                    AnnotationEntity annotation = annotationService.retrieve(
                        ref.getId()
                    );
                    wrapper.addRelated(annotation.toDTO());
                }
            }
        }
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