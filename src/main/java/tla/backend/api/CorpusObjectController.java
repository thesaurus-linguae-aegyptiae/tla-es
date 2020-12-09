package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.service.CorpusObjectService;
import tla.backend.service.EntityService;
import tla.domain.dto.CorpusObjectDto;

@RestController
@RequestMapping("/object")
public class CorpusObjectController extends EntityController<CorpusObjectEntity, CorpusObjectDto> {

    @Autowired
    private CorpusObjectService service;

    @Override
    public EntityService<CorpusObjectEntity, ?, CorpusObjectDto> getService() {
        return service;
    }

}