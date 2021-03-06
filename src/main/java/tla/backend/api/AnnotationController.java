package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.AnnotationEntity;
import tla.backend.service.AnnotationService;
import tla.backend.service.EntityService;
import tla.domain.dto.AnnotationDto;

@RestController
@RequestMapping("/annotation")
public class AnnotationController extends EntityController<AnnotationEntity, AnnotationDto> {

    @Autowired
    private AnnotationService queryService;

    @Override
    public EntityService<AnnotationEntity, ?, AnnotationDto> getService() {
        return queryService;
    }

}