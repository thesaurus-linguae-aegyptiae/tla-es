package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.Metadata;

public interface MetadataRepo extends ElasticsearchRepository<Metadata, String> {}