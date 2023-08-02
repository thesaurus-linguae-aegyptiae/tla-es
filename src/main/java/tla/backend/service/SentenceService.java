package tla.backend.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.service.component.EntityRetrieval;
import tla.domain.command.SearchCommand;
import tla.domain.command.SentenceSearch;
import tla.domain.dto.SentenceDto;
import tla.domain.dto.extern.SearchResultsWrapper;
import java.util.List;
import java.util.Collections;
import org.elasticsearch.index.query.BoolQueryBuilder;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import java.util.stream.Collectors;
@Service
@ModelClass(value = SentenceEntity.class, path = "sentence")
public class SentenceService extends EntityService<SentenceEntity, ElasticsearchRepository<SentenceEntity, String>, SentenceDto> {
	 private final ElasticsearchOperations elasticsearchOperations;
	 private static final Logger logger = LoggerFactory.getLogger(SentenceService.class);

    @Autowired
    private SentenceRepo repo;
    
    public SentenceService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public ElasticsearchRepository<SentenceEntity, String> getRepo() {
        return repo;
    }
    public List<SentenceEntity> searchSentencesByContextTextId(String textId) {
        SentenceSearchQueryBuilder queryBuilder = new SentenceSearchQueryBuilder();
        queryBuilder.setTextIds(Collections.singletonList(textId));
      
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder.getQueryBuilder())
                .build();

        SearchHits<SentenceEntity> searchHits = elasticsearchOperations.search(
                searchQuery,
                SentenceEntity.class,
                elasticsearchOperations.getIndexCoordinatesFor(SentenceEntity.class)
        );

        return searchHits.stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());
    }


    /**
     * make sure containing text gets included.
     */
    @Override
    protected EntityRetrieval.BulkEntityResolver retrieveRelatedDocs(SentenceEntity document) {
        EntityRetrieval.BulkEntityResolver relatedDocuments = super.retrieveRelatedDocs(document);
        var text = this.retrieveSingleBTSDoc(
            "BTSText", document.getContext().getTextId()
        );
        relatedDocuments.merge(
            super.retrieveReferencedThesaurusEntries(text)
        );
        return relatedDocuments;
    }

    @Override
    public Class<? extends ESQueryBuilder> getSearchCommandAdapterClass(SearchCommand<SentenceDto> command) {
        return SentenceSearchQueryBuilder.class;
    }

}