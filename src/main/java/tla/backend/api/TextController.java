package tla.backend.api;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.TextEntity;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.backend.service.EntityService;
import tla.backend.service.TextService;
import tla.backend.service.search.SearchService;
import tla.domain.command.TextSearch;

@Slf4j
@RestController
@RequestMapping("/text")
public class TextController extends EntityController<TextEntity> {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TextService textService;

    @Autowired
    private SearchService searchService;

    @Override
    public EntityService<TextEntity, ?, ?> getService() {
        return textService;
    }

    @RequestMapping(
        value = "/query",
        method = RequestMethod.POST
    )
    public ResponseEntity<?> doQuery(@RequestBody TextSearch command) {
        log.info("incoming command: {}", tla.domain.util.IO.json(command));
        var query = modelMapper.map(command, TextSearchQueryBuilder.class);
        var result = searchService.register(query);
        return new ResponseEntity<>(
            result, HttpStatus.OK
        );
    }

}
