package tla.backend.api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.Lemma;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.error.ObjectNotFoundException;

@RestController
@RequestMapping("/lemma")
public class LemmaController {

    @Autowired
    private LemmaRepo repo;

    @RequestMapping("/get/{id}")
    public Lemma getLemmaById(@PathVariable String id) throws ObjectNotFoundException {
        // https://stackoverflow.com/a/35402975/1933494
        Optional<Lemma> result = repo.findById(id);
        if (!result.isEmpty()) {
            return result.get();
        }
        throw new ObjectNotFoundException();
    }    

    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public Lemma postLemma(@RequestBody Lemma lemma) {
        return repo.save(lemma);
    }
}
