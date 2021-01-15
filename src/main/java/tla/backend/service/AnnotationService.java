package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.repo.AnnotationRepo;
import tla.domain.command.SearchCommand;
import tla.domain.dto.AnnotationDto;

@Service
@ModelClass(value = AnnotationEntity.class, path = "annotation")
public class AnnotationService extends EntityService<AnnotationEntity, ElasticsearchRepository<AnnotationEntity, String>, AnnotationDto> {

    @Autowired
    private AnnotationRepo repo;

    @Override
    public ElasticsearchRepository<AnnotationEntity, String> getRepo() {
        return repo;
    }

    @Override
    public ESQueryBuilder getSearchCommandAdapter(SearchCommand<AnnotationDto> command) {
        return null;
    }

}