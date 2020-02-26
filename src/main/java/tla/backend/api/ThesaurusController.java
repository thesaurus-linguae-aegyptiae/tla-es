package tla.backend.api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.error.ObjectNotFoundException;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.ThesaurusRepo;

@RestController
@RequestMapping("/ths")
public class ThesaurusController {

    @Autowired
    private ThesaurusRepo repo;

    @RequestMapping(method = RequestMethod.GET, value = "/get/{id}")
    public ResponseEntity<ThsEntryEntity> getEntry(@PathVariable String id) throws ObjectNotFoundException {
        Optional<ThsEntryEntity> result = repo.findById(id);
        if (!result.isEmpty()) {
            return new ResponseEntity<ThsEntryEntity>(
                result.get(),
                HttpStatus.OK
            );
        }
        throw new ObjectNotFoundException();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/post")
    public ResponseEntity<ThsEntryEntity> postEntry(@RequestBody ThsEntryEntity entry) {
        return new ResponseEntity<ThsEntryEntity>(
            repo.save(entry),
            HttpStatus.CREATED
        );
    }

}