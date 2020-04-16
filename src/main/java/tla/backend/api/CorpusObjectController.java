package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.service.CorpusObjectService;
import tla.backend.service.QueryService;

@RestController
@RequestMapping("/object")
public class CorpusObjectController extends EntityController<CorpusObjectEntity> {

    @Autowired
    private CorpusObjectService service;

    @Override
    public QueryService<CorpusObjectEntity> getService() {
        return service;
    }

}