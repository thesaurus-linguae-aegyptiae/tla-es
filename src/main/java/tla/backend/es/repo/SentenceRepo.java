package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.SentenceEntity;

public interface SentenceRepo extends ElasticsearchRepository<SentenceEntity, String> {}