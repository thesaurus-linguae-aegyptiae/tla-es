package tla.backend.es.repo;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import tla.backend.es.model.CommentEntity;

public interface CommentRepo extends ElasticsearchRepository<CommentEntity, String> {}