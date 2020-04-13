package tla.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
    public AnnotationEntity retrieve(String id) {
        Optional<AnnotationEntity> result = repo.findById(id);
        if (result.isPresent()) {
            return result.get();
        } else {
            return null;
        }
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