package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.OccurrenceEntity;
import tla.backend.es.repo.OccurrenceRepo;

@Service
@ModelClass(value = OccurrenceEntity.class, path = "occurrence")
public class OccurrenceService extends QueryService<OccurrenceEntity> {

    @Autowired
    private OccurrenceRepo repo;

    @Override
    public ElasticsearchRepository<OccurrenceEntity, String> getRepo() {
        return repo;
    }

}