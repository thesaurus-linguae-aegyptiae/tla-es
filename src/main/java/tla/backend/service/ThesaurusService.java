package tla.backend.service;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.query.AbstractEntityQueryBuilder;
import tla.backend.es.repo.ThesaurusRepo;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;
import tla.domain.command.SearchCommand;
import tla.domain.dto.ThsEntryDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.Passport;

@Service
@ModelClass(value = ThsEntryEntity.class, path = "ths")
public class ThesaurusService extends UserFriendlyEntityService<ThsEntryEntity, UserFriendlyEntityRepo<ThsEntryEntity, String>, ThsEntryDto> {

    @Autowired
    private ThesaurusRepo thsRepo;

    @Override
    public UserFriendlyEntityRepo<ThsEntryEntity, String> getRepo() {
        return thsRepo;
    }

    /**
     * If a passport references thesaurus entries at the leaf node located by the
     * specified path, these thesaurus entries get retrieved from the index.
     *
     * @param passport
     * @param path
     * @return
     */
    public List<ThsEntryEntity> extractThsEntriesFromPassport(Passport passport, String path) {
        List<Passport> leafNodes = passport.extractProperty(path);
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
        });
        return terms;
    }

    @Override
    protected AbstractEntityQueryBuilder<?, ?> getEntityQueryBuilder(SearchCommand<?> search) {
        // TODO Auto-generated method stub
        return null;
    }

}