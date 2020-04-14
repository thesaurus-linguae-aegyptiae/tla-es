package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.repo.AnnotationRepo;
import tla.domain.dto.AnnotationDto;
import tla.domain.dto.extern.SingleDocumentWrapper;

@Service
public class AnnotationService extends QueryService<AnnotationEntity> {

    @Autowired
    private AnnotationRepo repo;

    @Override
    public ElasticsearchRepository<AnnotationEntity, String> getRepo() {
        return repo;
    }

    public SingleDocumentWrapper<AnnotationDto> getDetails(String id) {
        AnnotationEntity annotation = this.retrieve(id);
        if (annotation != null) {
            SingleDocumentWrapper<AnnotationDto> wrapper = new SingleDocumentWrapper<>(
                (AnnotationDto) annotation.toDTO()
            );
            return wrapper;
        } else {
            return null;
        }
    }

}