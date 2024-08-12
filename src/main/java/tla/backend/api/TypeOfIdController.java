package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


@RestController
@RequestMapping("/typeofid")
public class TypeOfIdController {
	
	//Controller Beginn 
	@Autowired
	private LemmaController lemmaController;
	
	@Autowired
	private SentenceController sentenceController;
	
	@Autowired
	private TextController textController;
	
	@Autowired
	private CorpusObjectController corpusObjectController;

	@Autowired
	private ThesaurusController thesaurusController;
	
	@Autowired
	private TokenController tokenController;
	//Controller Ende

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;
    
    /**
     * checks if id exists and returns index name
     */
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            consumes = MediaType.ALL_VALUE,
            produces = MediaType.ALL_VALUE
        )
        public ResponseEntity<String> getTypeOfId(@PathVariable String id){
    	//TODO List of Controller aus Vererbung
    	EntityController [] controllers = {
    			lemmaController, 
    			sentenceController,
    			textController,
    			corpusObjectController,
    			thesaurusController,
    			tokenController
    	};      
				for(EntityController controller : controllers) {
					System.out.println(controller.existsById(id));
        			if(controller.existsById(id)) {
        				  return new ResponseEntity<String>(
        						  controller.getPath(),
        		                    HttpStatus.OK
        		                );
        			}
        		}        		
                return new ResponseEntity<String>(
                    "false",
                    HttpStatus.OK
                );
            }
}
