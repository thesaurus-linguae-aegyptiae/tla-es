package tla.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import tla.backend.es.model.SentenceEntity;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.repo.SentenceRepo;
import tla.backend.service.component.EntityRetrieval;
import tla.domain.command.SearchCommand;
import tla.domain.dto.SentenceDto;

@Service
@ModelClass(value = SentenceEntity.class, path = "sentence")
public class SentenceService extends EntityService<SentenceEntity, ElasticsearchRepository<SentenceEntity, String>, SentenceDto> {

    @Autowired
    private SentenceRepo repo;

    @Override
    public ElasticsearchRepository<SentenceEntity, String> getRepo() {
        return repo;
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