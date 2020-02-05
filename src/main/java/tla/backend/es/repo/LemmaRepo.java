package tla.backend.es.repo;

import tla.backend.es.model.Lemma;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LemmaRepo extends ElasticsearchRepository<Lemma, String> {

    
}