package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.error.ObjectNotFoundException;
import tla.backend.service.ThesaurusService;
import tla.domain.dto.DocumentDto;
import tla.domain.dto.extern.SingleDocumentWrapper;

@RestController
@RequestMapping("/ths")
public class ThesaurusController {

    @Autowired
    private ThesaurusService thsService;

    @RequestMapping(method = RequestMethod.GET, value = "/get/{id}")
    public ResponseEntity<SingleDocumentWrapper<DocumentDto>> getEntry(@PathVariable String id) throws ObjectNotFoundException {
        SingleDocumentWrapper<DocumentDto> result = thsService.getDetails(id);
        if (result != null) {
            return new ResponseEntity<SingleDocumentWrapper<DocumentDto>> (
                result,
                HttpStatus.OK
            );
        }
        throw new ObjectNotFoundException();
    }

}