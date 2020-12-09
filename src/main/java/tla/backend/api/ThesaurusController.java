package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.ThsEntryEntity;
import tla.backend.service.EntityService;
import tla.backend.service.ThesaurusService;
import tla.domain.dto.ThsEntryDto;

@RestController
@RequestMapping("/ths")
public class ThesaurusController extends EntityController<ThsEntryEntity, ThsEntryDto> {

    @Autowired
    private ThesaurusService thsService;

    @Override
    public EntityService<ThsEntryEntity, ?, ThsEntryDto> getService() {
        return thsService;
    }

}