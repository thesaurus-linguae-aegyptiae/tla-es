package tla.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.Map.Entry;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.ModelConfig;
import tla.backend.es.repo.LemmaRepo;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.model.ObjectReference;

@Service
public class LemmaService extends QueryService<LemmaEntity> {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private ModelMapper modelMapper;

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
     * TODO make this generic for all entity types either in queryservice or tlaentity
     */
    private LemmaDto toDTO(LemmaEntity lemma) {
        return modelMapper.map(
            lemma,
            LemmaDto.class
        );
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
        for (Entry<String, List<ObjectReference>> e : lemma.getRelations().entrySet()) {
            List<ObjectReference> objectReferences = e.getValue();
            for (ObjectReference ref : objectReferences) {
                if (ref.getEclass().equals(ModelConfig.getEclass(LemmaEntity.class))) {
                    LemmaEntity relatedLemma = retrieve(
                        ref.getId()
                    );
                    wrapper.addRelated(toDTO(relatedLemma));
                }
            }
        }
        return wrapper;
    }

}