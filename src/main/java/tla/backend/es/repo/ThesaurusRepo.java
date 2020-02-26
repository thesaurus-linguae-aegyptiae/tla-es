package tla.backend.es.repo;

import tla.backend.es.model.ThsEntryEntity;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ThesaurusRepo extends ElasticsearchRepository<ThsEntryEntity, String> {

}