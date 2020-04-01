package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.OccurrenceEntity;

public interface OccurrenceRepo extends ElasticsearchRepository<OccurrenceEntity, String> {

}