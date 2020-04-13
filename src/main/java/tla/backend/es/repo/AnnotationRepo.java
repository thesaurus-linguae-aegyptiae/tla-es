package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.AnnotationEntity;

public interface AnnotationRepo extends ElasticsearchRepository<AnnotationEntity, String> {}