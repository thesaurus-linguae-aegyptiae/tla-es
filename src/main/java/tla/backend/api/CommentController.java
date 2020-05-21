package tla.backend.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tla.backend.es.model.CommentEntity;
import tla.backend.service.CommentService;
import tla.backend.service.QueryService;

@RestController
@RequestMapping("/comment")
public class CommentController extends EntityController<CommentEntity> {

    @Autowired
    private CommentService service;
    

    @Override
    public QueryService<CommentEntity> getService() {
        return this.service;
    }

}