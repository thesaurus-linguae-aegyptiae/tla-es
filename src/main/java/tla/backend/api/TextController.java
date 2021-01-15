package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.TextEntity;
import tla.backend.service.EntityService;
import tla.backend.service.TextService;
import tla.domain.dto.TextDto;

@RestController
@RequestMapping("/text")
public class TextController extends EntityController<TextEntity, TextDto> {

    @Autowired
    private TextService textService;

    @Override
    public EntityService<TextEntity, ?, TextDto> getService() {
        return textService;
    }

}