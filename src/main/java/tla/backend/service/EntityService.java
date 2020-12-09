package tla.backend.service;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.LinkedEntity;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.query.AbstractEntityIDsQueryBuilder;
import tla.backend.es.query.AbstractEntityQueryBuilder;
import tla.backend.es.query.ESQueryBuilder;
import tla.backend.es.query.ESQueryResult;
import tla.backend.service.component.EntityRetrieval;
import tla.backend.service.search.AutoCompleteSupport;
import tla.backend.service.search.SearchService;
import tla.domain.command.SearchCommand;
import tla.domain.dto.extern.PageInfo;
import tla.domain.dto.extern.SearchResultsWrapper;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.dto.meta.DocumentDto;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;
import tla.error.ObjectNotFoundException;

/**
 * Implementing subclasses must be annotated with {@link ModelClass} and be instantiated
 * using the no-args default constructor.
 * They should also be annotated with {@link Service} for component scanning / dependency injection.
 */
@Slf4j
public abstract class EntityService<T extends Indexable, R extends ElasticsearchRepository<T, String>, D extends AbstractDto> {

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ModelMapper modelMapper;

    private Class<T> modelClass = null;
    private Class<D> dtoClass = null;
    protected static Map<Class<? extends Indexable>, EntityService<? extends Indexable, ? extends ElasticsearchRepository<?,?>, ? extends AbstractDto>> modelClassServices = new HashMap<>();
    protected static Map<Class<? extends Indexable>, AbstractDto> modelClassDtos = new HashMap<>();

    /**
     * Default constructor registering services under the eclass specified via a {@link BTSeClass}
     * annotation.
     */
    @SuppressWarnings("unchecked")
    protected EntityService() {
        for (Annotation a : this.getClass().getAnnotationsByType(ModelClass.class)) {
            this.modelClass = (Class<T>) ((ModelClass) a).value();
            modelClassServices.put(
                this.modelClass,
                this
            );
            if (AbstractBTSBaseClass.class.isAssignableFrom(this.modelClass)) {
                try {
                    ModelConfig.registerModelClass(
                        this.modelClass.asSubclass(AbstractBTSBaseClass.class)
                    );
                } catch (Exception e) {
                    log.error(
                        String.format("failed to register model class %s", this.modelClass),
                        e
                    );
                }
            }
        }
    }

    public ModelMapper getModelMapper() {
        return this.modelMapper;
    }

    /**
     * Returns the domain class a service is taking care of (extracted from its
     * {@link ModelClass} annotation.
     */
    public Class<T> getModelClass() {
        return this.modelClass;
    }

    /**
     * Returns the entity service registered for a given model class, or null if no such model class have been
     * registered.
     * Registration takes place at construction time of any service with a {@link ModelClass} annotation.
     * This is required for the entity retrieval service to work.
     */
    public static EntityService<? extends Indexable, ? extends ElasticsearchRepository<?, ?>, ? extends AbstractDto> getService(
        Class<? extends AbstractBTSBaseClass> modelClass
    ) {
        if (modelClassServices.containsKey(modelClass)) {
            return modelClassServices.get(modelClass);
        } else {
            log.error("No service registered for eclass '{}'!'", modelClass);
            return null;
        }
    }

    /**
     * Return the class specified via {@link TLADTO} annotation on top
     * of the model class a service is for.
     *
     * @see #getModelClass()
     */
    @SuppressWarnings("unchecked")
    public Class<D> getDtoClass() {
        if (this.dtoClass != null) {
            return this.dtoClass;
        } else {
            for (Annotation a : getModelClass().getAnnotationsByType(TLADTO.class)) {
                this.dtoClass = (Class<D>) ((TLADTO) a).value();
                return this.dtoClass;
            }
        }
        return null;
    }

    /**
     * Returns all model classes for which there are services registered.
     *
     * @see {@link ModelClass} annotation
     */
    public static Collection<Class<? extends Indexable>> getRegisteredModelClasses() {
        return modelClassServices.keySet();
    }

    /**
     * Subclasses must implement this method and return their respective entity repository
     * (which they presumably got autowired directly into them via spring dependency injection).
     *
     * @return The {@link ElasticsearchRepository} instance used by this service to manage the
     * Elasticsearch index where documents of the model class with which the service has been
     * typed are stored in.
     */
    public abstract R getRepo();

    /**
     * get a service's {@link AutoCompleteSupport} instance for performing
     * quick lookup searches against its entity type. Can and should be overridden by
     * subclasses so that autocompletion for specific entity types can be customized.
     */
    public AutoCompleteSupport getAutoCompleteSupport() {
        return AutoCompleteSupport.DEFAULT;
    }

    /**
     * Retrieve a single document from the Elasticsearch index managed by the service's
     * {@link ElasticsearchRepository}, or return null if none can be found for the given ID.
     *
     * @param id Document ID
     * @return Either an instance of the model class with which the service has been typed,
     * or null if no such document with the given ID could be found.
     */
    public T retrieve(String id) {
        Optional<T> result = getRepo().findById(id);
        return result.isPresent() ? result.get() : null;
    }

    /**
     * Retrieves the document specified by its ID from the Elasticsearch index, converts it into
     * an instance of its class's corresponding DTO type, wraps a {@link SingleDocumentWrapper}
     * around it, and then attempts to instantiate all documents that are referenced by it,
     * be it via <code>passport</code> or <code>relations</code>.
     *
     * @param id Identifier of the reqeusted document.
     * @return A {@link SingleDocumentWrapper} container object wrapping the DTO-turned document,
     * and inshallah all documents referenced by it.
     * @see {@link ModelConfig#toDTO}
     */
    public SingleDocumentWrapper<? extends AbstractDto> getDetails(String id) {
        T document = this.retrieve(id);
        final SingleDocumentWrapper<? extends AbstractDto> container;
        if (document != null) {
            container = new SingleDocumentWrapper<AbstractDto>(
                ModelConfig.toDTO(document)
            );
            var bulk = this.retrieveRelatedDocs(document);
            if (document instanceof TLAEntity) {
                bulk.addAll(
                    ((TLAEntity) document).getPassport() != null ?
                    ((TLAEntity) document).getPassport().extractObjectReferences() : null
                );
            }
            try {
                bulk.resolve().forEach(
                    relatedObject -> {
                        container.addRelated(
                            (DocumentDto) ModelConfig.toDTO(relatedObject)
                        );
                    }
                );
            } catch (Exception e) {
                log.error("something went wrong during conversion of related objects to DTO");
            }
        } else {
            container = null;
        }
        return container;
    }

    /**
     * If a document if an instance of {@link LinkedEntity}, go through its
     * {@link LinkedEntity#getRelations()} data structure and
     * try to look up all objects referenced in their respective Elasticsearch indices based on
     * their <code>eclass</code> value.
     *
     * @param document An instance of the model class this service is for
     * @return A list of the document instances referenced. Returns an empty list if the document
     * passed is not a {@link LinkedEntity} instance.
     */
    protected EntityRetrieval.BulkEntityResolver retrieveRelatedDocs(T document) {
        return (document instanceof LinkedEntity) ? EntityRetrieval.BulkEntityResolver.from(
            (LinkedEntity) document
        ) : new EntityRetrieval.BulkEntityResolver();
    }

    /**
     * If a document is of type {@link TLAEntity}, then extract all document references
     * from its passport and try to retrieve them from the respective Elasticsearch indices
     * where those can be expected. Usually, the documents referenced from within a passport
     * should be thesaurus terms, and thus be located in the Elasticsearch index corresponding
     * to the eclass <code>"BTSThsEntry"</code>.
     */
    protected EntityRetrieval.BulkEntityResolver retrieveReferencedThesaurusEntries(Indexable document) {
        return ThesaurusService.extractThsEntriesFromPassport(document);
    }

    /**
     * Creates and executes native query from an Elasticsearch query builder against whatever index
     * is known for storing documents of the type specified via the <code>entityClass</code> parameter.
     *
     * @Deprecated
     */
    public SearchResponse query(
        Class<? extends Indexable> entityClass,
        QueryBuilder queryBuilder,
        AggregationBuilder aggsBuilder
    ) {
        return searchService.query(entityClass, queryBuilder, aggsBuilder);
    }

    /**
     * Tries to find the ES document identified by eclass and ID.
     */
    public Indexable retrieveSingleBTSDoc(String eclass, String id) {
        Class<? extends AbstractBTSBaseClass> modelClass = ModelConfig.getModelClass(eclass);
        EntityService<?, ?, ?> service = EntityService.getService(modelClass);
        if (service == null) {
            log.error("Could not find entity service for eclass {}!", eclass);
            throw new ObjectNotFoundException(id, eclass);
        }
        ElasticsearchRepository<? extends Indexable, String> repo = service.getRepo();
        Optional<? extends Indexable> result = repo.findById(id);
        return result.orElseThrow(
            () -> {
                log.error(
                    String.format(
                        "Could not retrieve %s document with ID %s",
                        eclass,
                        id
                    )
                );
                return new ObjectNotFoundException(id, eclass);
            }
        );
    }

    /**
     * Maps a bunch of model entities to DTOs.
     */
    public Collection<? extends AbstractDto> toDTO(Collection<T> entities) {
        return entities.stream().map(
            ModelConfig::toDTO
        ).collect(Collectors.toList());
    }

    /**
     * Try to find a bunch of domain objects in an ES index by running a query.
     * @Deprecated
     */
    public SearchHits<T> search(Query query) {
        log.info("query: {}", tla.domain.util.IO.json(query));
        return operations.search(
            query,
            getModelClass(),
            operations.getIndexCoordinatesFor(getModelClass())
        );
    }

    /**
     * Create a serializable transfer object containing page information
     * about an ES search result.
     *
     * @Deprecated
     */
    public PageInfo pageInfo(SearchHits<?> hits, Pageable pageable) {
        return ESQueryResult.pageInfo(hits, pageable, 0);
    }

    /**
     * Converts terms aggregations to a map of field value counts.
     *
     * @Deprecated
     */
    public Map<String, Map<String, Long>> facets(SearchHits<?> hits) {
        return searchService.extractFacets(hits);
    }

    /**
     * Extract DTO objects out of a list of searchresults of an entity type.
     */
    public List<D> hitsToDTO(SearchHits<?> hits, Class<D> dtoClass) {
        return hits.getSearchHits().stream().map(
            hit -> modelMapper.map(
                hit.getContent(),
                dtoClass
            )
        ).collect(Collectors.toList());
    }

    /**
     * Takes an Elasticsearch search result and the original page information and search
     * command, and puts it all into a serializable container ready for return
     * to the requesting client.
     */
    public SearchResultsWrapper<D> wrapSearchResults(
        SearchHits<?> hits, Pageable pageable, SearchCommand<D> command
    ) throws Exception {
        return new SearchResultsWrapper<D>(
            hitsToDTO(hits, getDtoClass()),
            command,
            pageInfo(hits, pageable),
            facets(hits)
        );
    }

    /**
     * Returns a list of matches for a simple multi field prefix query which
     * can be used for input field content assist and such. Can optionally filter
     * by object type.
     */
    public List<D> autoComplete(String type, String term) {
        if (term.length() < 3) {
            return Collections.emptyList();
        }
        return hitsToDTO(
            searchService.executeSearchQuery(
                this.getAutoCompleteSupport().autoCompleteQuery(term, type),
                this.getModelClass()
            ).getHits(),
            getDtoClass()
        );
    }

    /**
     * take a search command and based on the type figure out which
     * {@link AbstractEntityQueryBuilder entitiy query builder} or {@link
     * AbstractEntityIDsQueryBuilder entity ID query builder} you can put together
     * with this.
     * @deprecated
     */
    protected abstract AbstractEntityQueryBuilder<?, ?> getEntityQueryBuilder(
        SearchCommand<?> search
    );

    /**
     * @deprecated
     */
    public Optional<AbstractEntityQueryBuilder<?, ?>> findMatchingEntityQueryBuilder(
        SearchCommand<?> search, Class<? extends AbstractBTSBaseClass> target
    ) {
        EntityService<?, ?, ?> targetService = EntityService.getService(target);
        if (target != null) {
            return Optional.of(
                targetService.getEntityQueryBuilder(search)
            );
        } else {
            log.error(
                String.format(
                    "Could not find an entity query builder suitable for target entity type %s" +
                    " and incoming search command type %s!",
                    target, search
                )
            );
            return Optional.empty();
        }
    }

    /**
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public Optional<SearchResultsWrapper<D>> search(
        SearchCommand<? extends AbstractDto> command,
        Class<? extends AbstractBTSBaseClass> entityType,
        Pageable page
    ) {
        AbstractEntityQueryBuilder<?, ?> entityQueryBuilder = findMatchingEntityQueryBuilder(
            command,
            entityType
        ).orElseThrow(
            () -> new ObjectNotFoundException(
                command.getClass().getName()
            )
        );
        try {
            return Optional.of(
                wrapSearchResults(
                    search(
                        entityQueryBuilder.build(page)
                    ),
                    page,
                    (SearchCommand<D>) command
                )
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * subclasses should implement this by using {@link #getModelMapper()} to convert command into native query builder/adapter.
     */
    public abstract ESQueryBuilder getSearchCommandAdapter(SearchCommand<D> command);

    public Optional<SearchResultsWrapper<? extends D>> runSearchCommand(SearchCommand<D> command, Pageable page) {
        log.info("page: {}", page);
        var queryAdapter = this.getSearchCommandAdapter(command);
        var result = searchService.register(queryAdapter).run(page);
        try {
            var wrapper = new SearchResultsWrapper<D>(
                hitsToDTO(result.getHits(), this.getDtoClass()),
                command,
                result.getPageInfo(),
                facets(result.getHits())
            );
            return Optional.of(wrapper);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}