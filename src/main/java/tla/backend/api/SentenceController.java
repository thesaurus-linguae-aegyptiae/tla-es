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
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.service.EntityService;
import tla.backend.service.SentenceService;
import tla.backend.service.search.SearchService;
import tla.domain.command.SentenceSearch;

@Slf4j
@RestController
@RequestMapping("/sentence")
public class SentenceController extends EntityController<SentenceEntity> {

    @Autowired
    private SentenceService service;

    @Autowired
    private SearchService search;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public EntityService<SentenceEntity, ?, ?> getService() {
        return this.service;
    }

    @RequestMapping(
        value = "query",
        method = RequestMethod.POST
    )
    public ResponseEntity<?> doQuery(@RequestBody SentenceSearch command) {
        log.info("incoming command: {}", tla.domain.util.IO.json(command));
        var query = modelMapper.map(command, SentenceSearchQueryBuilder.class);
        var result = search.register(query);
        return new ResponseEntity<>(
            result, HttpStatus.OK
        );
    }


}