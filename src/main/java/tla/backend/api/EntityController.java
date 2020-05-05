package tla.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tla.backend.error.ObjectNotFoundException;
import tla.backend.es.model.meta.Indexable;
import tla.backend.service.QueryService;
import tla.domain.dto.DocumentDto;
import tla.domain.dto.extern.SingleDocumentWrapper;

/**
 * Generic TLA entity REST controller.
 *
 * Subclasses should be annotated with a path mapping, e.g. <code>@RequestMapping("/ths")</code>.
 * They also need to be annotated with {@link RestController}.
 */
@Slf4j
@RestController
public abstract class EntityController<T extends Indexable> {

    /**
     * Must return a presumably autowired entity service of appropriate type.
     */
    public abstract QueryService<T> getService();

    /**
     * Returns a document wrapper containing a single document and all documents it references.
     */
    @RequestMapping(
        value = "/get/{id}",
        method = RequestMethod.GET,
        consumes = MediaType.ALL_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<SingleDocumentWrapper<DocumentDto>> get(@PathVariable String id) throws ObjectNotFoundException {
        SingleDocumentWrapper<DocumentDto> result = getService().getDetails(id);
        if (result != null) {
            return new ResponseEntity<SingleDocumentWrapper<DocumentDto>>(
                result,
                HttpStatus.OK
            );
        }
        log.error("could not find annotation {}", id);
        throw new ObjectNotFoundException();
    }

}