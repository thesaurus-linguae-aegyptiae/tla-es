package tla.backend.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.service.LemmaService;
import tla.backend.service.QueryService;
import tla.domain.command.LemmaSearch;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.PageInfo;
import tla.domain.dto.extern.SearchResultsWrapper;


@Slf4j
@RestController
@RequestMapping("/lemma")
public class LemmaController extends EntityController<LemmaEntity> {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private LemmaService queryService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public QueryService<LemmaEntity> getService() {
        return queryService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/count")
    public ResponseEntity<Long> countLemmata() {
        log.debug("counting lemmata: {}", repo.count());
        return new ResponseEntity<Long>(
            repo.count(),
            HttpStatus.OK
        );
    }

    @RequestMapping(method = RequestMethod.GET, value = "/frequencies")
    public ResponseEntity<Map<String, Long>> getFrequencies() {
        Map<String, Long> freq = queryService.getMostFrequent(2000);
        return new ResponseEntity<Map<String, Long>>(
            freq,
            HttpStatus.OK
        );
    }

    @RequestMapping(method = RequestMethod.GET, value = "/get")
    public ResponseEntity<Iterable<LemmaDto>> getLemmataById(@RequestParam List<String> ids) {
        List<LemmaDto> results = new ArrayList<>();
        repo.findAllById(ids).forEach(
            entity -> {
                results.add(
                    modelMapper.map(
                        entity,
                        LemmaDto.class
                    )
                );
            }
        );
        return new ResponseEntity<Iterable<LemmaDto>>(
            results,
            HttpStatus.OK
        );
    }

    @RequestMapping(method = RequestMethod.GET, value = "/all")
    public Iterable<LemmaEntity> getAll() {
        return repo.findAll(Sort.by("sortKey"));
    }

    @RequestMapping(
        value = "/search",
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SearchResultsWrapper<LemmaDto>> search(@RequestBody LemmaSearch command, Pageable pageable) throws Exception {
        log.info("page: {}", pageable);
        Page<LemmaEntity> page = queryService.search(
            queryService.createLemmaSearchQuery(command, pageable)
        );
        return new ResponseEntity<>(
            new SearchResultsWrapper<>(
                page.getContent().stream().map(
                    entity -> modelMapper.map(
                        entity,
                        LemmaDto.class
                    )
                ).collect(Collectors.toList()),
                command,
                modelMapper.map(
                    page,
                    PageInfo.class
                )
            ),
            HttpStatus.OK
        );
    }

}
