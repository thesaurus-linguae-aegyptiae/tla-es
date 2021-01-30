package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.CommentEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.repo.CommentRepo;
import tla.domain.command.SearchCommand;
import tla.domain.dto.CommentDto;

@Service
@ModelClass(value = CommentEntity.class, path = "comment")
public class CommentService extends EntityService<CommentEntity, ElasticsearchRepository<CommentEntity, String>, CommentDto> {

    @Autowired
    private CommentRepo repo;

    @Override
    public ElasticsearchRepository<CommentEntity, String> getRepo() {
        return this.repo;
    }

    @Override
    public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<CommentDto> command) {
        // TODO
        return null;
    }
}