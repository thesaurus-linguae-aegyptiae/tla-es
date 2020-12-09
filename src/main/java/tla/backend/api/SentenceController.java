package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.SentenceEntity;
import tla.backend.service.EntityService;
import tla.backend.service.SentenceService;
import tla.domain.dto.SentenceDto;

@RestController
@RequestMapping("/sentence")
public class SentenceController extends EntityController<SentenceEntity, SentenceDto> {

    @Autowired
    private SentenceService service;

    @Override
    public EntityService<SentenceEntity, ?, SentenceDto> getService() {
        return this.service;
    }

}