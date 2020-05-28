package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.ThsEntryEntity;
import tla.backend.service.EntityService;
import tla.backend.service.ThesaurusService;

@RestController
@RequestMapping("/ths")
public class ThesaurusController extends EntityController<ThsEntryEntity> {

    @Autowired
    private ThesaurusService thsService;

    @Override
    public EntityService<ThsEntryEntity> getService() {
        return thsService;
    }

}