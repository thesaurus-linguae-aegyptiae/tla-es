package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.repo.AnnotationRepo;

@Service
@ModelClass(value = AnnotationEntity.class, path = "annotation")
public class AnnotationService extends QueryService<AnnotationEntity> {

    @Autowired
    private AnnotationRepo repo;

    @Override
    public ElasticsearchRepository<AnnotationEntity, String> getRepo() {
        return repo;
    }

}