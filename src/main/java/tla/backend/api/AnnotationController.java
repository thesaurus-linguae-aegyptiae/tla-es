package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tla.backend.error.ObjectNotFoundException;
import tla.backend.service.AnnotationService;
import tla.domain.dto.AnnotationDto;
import tla.domain.dto.extern.SingleDocumentWrapper;

@Slf4j
@RestController
@RequestMapping("/annotation")
public class AnnotationController {

    @Autowired
    private AnnotationService queryService;

    @RequestMapping(method = RequestMethod.GET, value = "/get/{id}")
    public ResponseEntity<SingleDocumentWrapper<AnnotationDto>> get(@PathVariable String id) throws ObjectNotFoundException {
        SingleDocumentWrapper<AnnotationDto> container = queryService.getDetails(id);
        if (container != null) {
            return new ResponseEntity<SingleDocumentWrapper<AnnotationDto>>(
                container,
                HttpStatus.OK
            );
        }
        log.error("could not find annotation {}", id);
        throw new ObjectNotFoundException();
    }

}