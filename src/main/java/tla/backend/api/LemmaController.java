package tla.backend.api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.LemmaEntity;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.error.ObjectNotFoundException;

@RestController
@RequestMapping("/lemma")
public class LemmaController {

    @Autowired
    private LemmaRepo repo;

    @RequestMapping(method = RequestMethod.GET, value = "/get/{id}")
    public LemmaEntity getLemmaById(@PathVariable String id) throws ObjectNotFoundException {
        // https://stackoverflow.com/a/35402975/1933494
        Optional<LemmaEntity> result = repo.findById(id);
        if (!result.isEmpty()) {
            return result.get();
        }
        throw new ObjectNotFoundException();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/all")
    public Iterable<LemmaEntity> getAll() {
        return repo.findAll(Sort.by("sortKey"));
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
