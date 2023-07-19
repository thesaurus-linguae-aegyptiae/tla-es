package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.SentenceEntity;
import tla.backend.service.EntityService;
import tla.backend.service.TokenService;
import tla.domain.dto.SentenceDto;

@RestController
@RequestMapping("/token")
public class TokenController extends EntityController<SentenceEntity, SentenceDto> {

    @Autowired
    private TokenService service;

    @Override
    public EntityService<SentenceEntity, ?, SentenceDto> getService() {
        return this.service;
    }

}
