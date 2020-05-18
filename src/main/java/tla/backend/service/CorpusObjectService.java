package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.repo.CorpusObjectRepo;

@Service
@ModelClass(value = CorpusObjectEntity.class, path = "object")
public class CorpusObjectService extends QueryService<CorpusObjectEntity> {

    @Autowired
    private CorpusObjectRepo repo;

    @Override
    public ElasticsearchRepository<CorpusObjectEntity, String> getRepo() {
        return repo;
    }

}