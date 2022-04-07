package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TestRepo extends ElasticsearchRepository<RepoPopulatorTest.TestEntity, String> {}
