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
import tla.backend.service.TextService;
import tla.domain.dto.DocumentDto;
import tla.domain.dto.extern.SingleDocumentWrapper;

@Slf4j
@RestController
@RequestMapping("/text")
public class TextController {

    @Autowired
    private TextService textService;

    @RequestMapping(method = RequestMethod.GET, value = "/get/{id}")
    public ResponseEntity<SingleDocumentWrapper<DocumentDto>> getText(@PathVariable String id) throws ObjectNotFoundException {
        SingleDocumentWrapper<DocumentDto> result = textService.getDetails(id);
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
