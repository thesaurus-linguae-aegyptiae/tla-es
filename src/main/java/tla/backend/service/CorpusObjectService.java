package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.query.AbstractEntityQueryBuilder;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.backend.es.repo.CorpusObjectRepo;
import tla.backend.es.repo.custom.UserFriendlyEntityRepo;
import tla.domain.command.SearchCommand;
import tla.domain.dto.CorpusObjectDto;

@Service
@ModelClass(value = CorpusObjectEntity.class, path = "object")
public class CorpusObjectService extends
        UserFriendlyEntityService<CorpusObjectEntity, UserFriendlyEntityRepo<CorpusObjectEntity, String>, CorpusObjectDto> {

    @Autowired
    private CorpusObjectRepo repo;

    @Override
    public UserFriendlyEntityRepo<CorpusObjectEntity, String> getRepo() {
        return repo;
    }

    @Override
    protected AbstractEntityQueryBuilder<?, ?> getEntityQueryBuilder(SearchCommand<?> search) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ESQueryBuilder getSearchCommandAdapter(SearchCommand<CorpusObjectDto> command) {
        return this.getModelMapper().map(command, TextSearchQueryBuilder.class);
    }

}