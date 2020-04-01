package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.TextEntity;

public interface TextRepo extends ElasticsearchRepository<TextEntity, String> {}