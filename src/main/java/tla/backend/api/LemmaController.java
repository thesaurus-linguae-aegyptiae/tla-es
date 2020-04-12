package tla.backend.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.BaseEntity;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.OccurrenceEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.service.LemmaService;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.backend.error.ObjectNotFoundException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;

@Slf4j
@RestController
@RequestMapping("/lemma")
public class LemmaController {

    @Autowired
    private LemmaRepo repo;

    @Autowired
    private LemmaService queryService;

    @Autowired
    private ModelMapper modelMapper;

    @RequestMapping(method = RequestMethod.GET, value = "/count")
    public ResponseEntity<Long> countLemmata() {
        log.debug("counting lemmata: {}", repo.count());
        return new ResponseEntity<Long>(
            repo.count(),
            HttpStatus.OK
        );
    }

    /**
     * TODO this is just for debugging temporarily
     */
    @RequestMapping(method = RequestMethod.GET, value = "/find/{id}")
    public ResponseEntity<LemmaEntity> findLemmaById(@PathVariable String id) throws Exception {
        BaseEntity result = queryService.retrieveSingleBTSDoc("BTSLemmaEntry", id);
        if (result instanceof LemmaEntity) {
            return new ResponseEntity<LemmaEntity>(
                (LemmaEntity) result,
                HttpStatus.OK
            );
        }
        throw new ObjectNotFoundException();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/get/{id}")
    public ResponseEntity<SingleDocumentWrapper<LemmaDto>> getLemmaById(@PathVariable String id) throws ObjectNotFoundException {
        // https://stackoverflow.com/a/35402975/1933494
        SingleDocumentWrapper<LemmaDto> wrappedResult = queryService.getLemmaDetails(id);
        if (wrappedResult != null) {
            return new ResponseEntity<SingleDocumentWrapper<LemmaDto>>(
                wrappedResult,
                HttpStatus.OK
            );
        }
        log.warn("could not find lemma with ID {}!", id);
        throw new ObjectNotFoundException();
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

    @RequestMapping(method = RequestMethod.GET, value = "/{id}/texts")
    public ResponseEntity<List<String>> countOccurrencesInTexts(@PathVariable String id) throws IOException {
        TermQueryBuilder queryBuilder = QueryBuilders.termQuery(
            "lemma.id",
            id
        );
        TermsAggregationBuilder aggrBuilder = AggregationBuilders
            .terms("top_texts")
            .field("location.textId")
            .order(BucketOrder.count(false))
            .size(100000);
        SearchResponse response = queryService.query(
            OccurrenceEntity.class,
            queryBuilder,
            aggrBuilder
        );
        Terms topTexts = (Terms) response.getAggregations().asMap().get("top_texts");
        List<String> result = topTexts.getBuckets().stream()
            .map(
                b -> {return String.format("%s: %d", b.getKeyAsString(), b.getDocCount());}
            )
            .collect(
                Collectors.toList()
            );
        return new ResponseEntity<List<String>>(
            result,
            HttpStatus.OK
        );

    }

    @RequestMapping(method = RequestMethod.POST, value = "/post")
    public ResponseEntity<LemmaEntity> postLemma(@RequestBody LemmaEntity lemma) {
        return new ResponseEntity<LemmaEntity>(
            repo.save(lemma),
            HttpStatus.CREATED
        );
    }

    @RequestMapping(method = RequestMethod.POST, value = "/batch")
    public ResponseEntity<Iterable<LemmaEntity>> postLemma(@RequestBody Iterable<LemmaEntity> lemmata) {
        return new ResponseEntity<Iterable<LemmaEntity>>(
            repo.saveAll(lemmata),
            HttpStatus.CREATED
        );
    }

}
