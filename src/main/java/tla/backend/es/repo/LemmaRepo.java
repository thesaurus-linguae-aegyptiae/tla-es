package tla.backend.es.repo;

import tla.backend.es.model.LemmaEntity;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LemmaRepo extends ElasticsearchRepository<LemmaEntity, String> {

}