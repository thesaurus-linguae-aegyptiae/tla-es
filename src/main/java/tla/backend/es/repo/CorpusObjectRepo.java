package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.CorpusObjectEntity;

public interface CorpusObjectRepo extends ElasticsearchRepository<CorpusObjectEntity, String> {}