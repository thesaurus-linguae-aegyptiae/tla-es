package tla.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.SentenceEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.ESQueryResult;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.service.component.EntityRetrieval;
import tla.domain.command.SearchCommand;
import tla.domain.command.SentenceSearch;
import tla.domain.command.SentenceSearch.TokenSpec;
import tla.domain.dto.SentenceDto;
import tla.domain.model.SentenceToken;
import tla.backend.es.query.ESQueryResult;

@Service
@ModelClass(value = SentenceEntity.class, path = "token")
public class TokenService extends EntityService<SentenceEntity, ElasticsearchRepository<SentenceEntity, String>, SentenceDto> {

    @Autowired
    private SentenceRepo repo;
    

    @Override
    public ElasticsearchRepository<SentenceEntity, String> getRepo() {
        return repo;
    }  

    public Boolean existsById(String id) { 	
		SentenceSearch command= new SentenceSearch();
		TokenSpec tokenspec = new TokenSpec();
		tokenspec.setId(id);
		List<TokenSpec> tokens = new ArrayList<TokenSpec>();
		tokens.add(tokenspec);
		command.setTokens(tokens);
		//TODO think of move to controller
		Pageable page10 = PageRequest.of(0, 10);
		var queryAdapter = this.getSearchCommandAdapter(command);
		ESQueryResult<?> result = searchService.register(queryAdapter).run(page10);
		if (result.getHitCount() > 0) {
			return true;
		}	
		return false;
	}
    
    @Override
    public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<SentenceDto> command) {
        return SentenceSearchQueryBuilder.class;
    }

}