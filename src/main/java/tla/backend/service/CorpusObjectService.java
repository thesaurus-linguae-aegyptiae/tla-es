package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.query.AbstractEntityQueryBuilder;
import tla.backend.es.repo.CorpusObjectRepo;
import tla.domain.command.SearchCommand;
import tla.domain.dto.CorpusObjectDto;

@Service
@ModelClass(value = CorpusObjectEntity.class, path = "object")
public class CorpusObjectService extends EntityService<CorpusObjectEntity, CorpusObjectDto> {

    @Autowired
    private CorpusObjectRepo repo;

    @Override
    public ElasticsearchRepository<CorpusObjectEntity, String> getRepo() {
        return repo;
    }

    @Override
    protected AbstractEntityQueryBuilder<?, ?> getEntityQueryBuilder(SearchCommand<?> search) {
        // TODO Auto-generated method stub
        return null;
    }

}