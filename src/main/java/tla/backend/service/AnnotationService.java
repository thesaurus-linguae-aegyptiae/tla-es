package tla.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.repo.AnnotationRepo;

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

}