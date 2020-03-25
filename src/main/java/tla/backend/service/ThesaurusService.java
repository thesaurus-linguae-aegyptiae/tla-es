package tla.backend.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.ThesaurusRepo;
import tla.domain.model.ObjectReference;
import tla.domain.model.Passport;

@Service
public class ThesaurusService extends QueryService<ThsEntryEntity> {

    @Autowired
    private ThesaurusRepo thsRepo;


    /**
     * Retrieves single thesaurus entry from index. Returns null if there is none with the specified ID.
     */
    @Override
    public ThsEntryEntity retrieve(String id) {
        Optional<ThsEntryEntity> res = thsRepo.findById(id);
        if (res.isPresent()) {
            return res.get();
        } else {
            return null;
        }
    }

    /**
     * If a passport references thesaurus entries at the leaf node located by the specified path,
     * these thesaurus entries get retrieved from the index.
     * @param passport
     * @param path
     * @return
     */
    public List<ThsEntryEntity> extractThsEntriesFromPassport(Passport passport, String path) {
        List<Passport> leafNodes =  passport.extractProperty(path);
        List<ThsEntryEntity> terms = new LinkedList<>();
        leafNodes.stream().forEach(
            node -> {
                if (node.get() instanceof ObjectReference) {
                    terms.add(
                        this.retrieve(
                            ((ObjectReference) node.get()).getId()
                        )
                    );
                }
            }
        );
        return terms;
    }

}