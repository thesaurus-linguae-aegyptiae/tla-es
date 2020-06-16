package tla.backend.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.query.AbstractEntityIDsQueryBuilder;
import tla.backend.es.query.AbstractEntityQueryBuilder;
import tla.backend.service.search.AutoCompleteSupport;
import tla.domain.command.SearchCommand;
import tla.domain.dto.extern.PageInfo;
import tla.domain.dto.extern.SearchResultsWrapper;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.dto.meta.DocumentDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.Resolvable;
import tla.domain.model.meta.TLADTO;
import tla.error.ObjectNotFoundException;

/**
 * Implementing subclasses must be annotated with {@link ModelClass} and be instantiated
 * using the no-args default constructor.
 * They should also be annotated with {@link Service} for component scanning / dependency injection.
 */
@Slf4j
public abstract class EntityService<T extends Indexable, R extends ElasticsearchRepository<T, String>, D extends AbstractDto> {

    /**
     * How many search results fit in 1 page.
     */
    public static final int SEARCH_RESULT_PAGE_SIZE = 20;

    @Autowired
    protected RestHighLevelClient restClient;

    @Autowired
    private ElasticsearchOperations operations;

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
        }
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
     */
    public static EntityService<? extends Indexable, ? extends ElasticsearchRepository<?, ?>, ? extends AbstractDto> getService(Class<? extends Indexable> modelClass) {
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
     * Retrieves the docuement specified by its ID from the Elasticsearch index, converts it into
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
            Collection<BaseEntity> relatedObjects = new HashSet<>();
            relatedObjects.addAll(this.retrieveRelatedDocs(document));
            relatedObjects.addAll(this.retrieveReferencedThesaurusEntries(document));
            try {
                relatedObjects.forEach(
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
     * Retrieves the documents represented by a collection of {@link ObjectReference} instances
     * from their respective Elasticsearch indices.
     *
     * @param references The object references to be lookup up and instantiated
     * @return A List of TLA documents
     * @see {@link #retrieveSingleBTSDoc(String, String)}
     */
    protected Collection<BaseEntity> retrieveReferencedObjects(Collection<Resolvable> references) {
        return references.stream().map(
            ref -> {
                BaseEntity referencedEntity = this.retrieveSingleBTSDoc(
                    ref.getEclass(),
                    ref.getId()
                );
                if (referencedEntity == null) {
                    log.warn(
                        "Could not retrieve referenced object {}",
                        tla.domain.util.IO.json(ref)
                    );
                }
                return referencedEntity;
            }
        ).filter(
            entity -> entity != null
        )
        .collect(
            Collectors.toList()
        );
    }

    /**
     * If a document if an instance of {@link BaseEntity}, go through its
     * {@link BaseEntity#getRelations()} data structure and
     * try to look up all objects referenced in their respective Elasticsearch indices based on
     * their <code>eclass</code> value.
     *
     * @param document An instance of the model class this service is for
     * @return A list of the document instances referenced. Returns an empty list if the document
     * passed is not a {@link BaseEntity} instance.
     */
    protected Collection<BaseEntity> retrieveRelatedDocs(T document) {
        if (document instanceof BaseEntity) {
            Set<Resolvable> previews = new HashSet<>();
            BaseEntity entity = (BaseEntity) document;
            entity.getRelations().entrySet().forEach(
                e -> {previews.addAll(e.getValue());}
            );
            return retrieveReferencedObjects(previews);
        }
        return new ArrayList<BaseEntity>();
    }

    /**
     * If a document is of type {@link TLAEntity}, then extract all document references
     * from its passport and try to retrieve them from the respective Elasticsearch indices
     * where those can be expected. Usually, the documents referenced from within a passport
     * should be thesaurus terms, and thus be located in the Elasticsearch index corresponding
     * to the eclass <code>"BTSThsEntry"</code>.
     */
    protected Collection<BaseEntity> retrieveReferencedThesaurusEntries(T document) {
        if (document instanceof TLAEntity) {
            Set<Resolvable> previews = new HashSet<>();
            TLAEntity entity = (TLAEntity) document;
            if (entity.getPassport() != null) {
                previews.addAll(entity.getPassport().extractObjectReferences());
            }
            return retrieveReferencedObjects(previews);
        }
        return new ArrayList<BaseEntity>();
    }

    /**
     * Execute an Elasticsearch query against the index defined for <code>entityClass</code>.
     */
    public SearchResponse query(
        Class<? extends Indexable> entityClass,
        QueryBuilder queryBuilder,
        AggregationBuilder aggsBuilder
    ) {
        String index = operations.getIndexCoordinatesFor(entityClass).getIndexName();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
            .query(queryBuilder);
        if (aggsBuilder != null) {
            searchSourceBuilder = searchSourceBuilder.aggregation(aggsBuilder);
        }
        SearchRequest request = new SearchRequest()
            .indices(
                index
            )
            .source(
                searchSourceBuilder
            );
        SearchResponse response = null;
        try {
            response = restClient
                .search(
                    request,
                    RequestOptions.DEFAULT
                );
        } catch (Exception e) {
            log.error(
                String.format(
                    "could not query index %s",
                    index
                ),
                e
            );
        }
        return response;
    }

    /**
     * Tries to find the ES document identified by eclass and ID.
     */
    public BaseEntity retrieveSingleBTSDoc(String eclass, String id) {
        Class<? extends BaseEntity> modelClass = ModelConfig.getModelClass(eclass);
        EntityService<?, ?, ?> service = modelClassServices.getOrDefault(modelClass, null);
        if (service == null) {
            log.error("Could not find entity service for eclass {}!", eclass);
            return null;
        }
        ElasticsearchRepository<? extends Indexable, String> repo = service.getRepo();
        Optional<? extends Indexable> result = repo.findById(id);
        if (result.isPresent()) {
            if (result.get() instanceof BaseEntity) {
                return (BaseEntity) result.get();
            }
        }
        log.error(
            String.format(
                "Could not retrieve %s document with ID %s",
                eclass,
                id
            )
        );
        return null;
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
     */
    public PageInfo pageInfo(SearchHits<?> hits, Pageable pageable) {
        return PageInfo.builder()
            .number(pageable.getPageNumber())
            .totalElements(hits.getTotalHits())
            .size(EntityService.SEARCH_RESULT_PAGE_SIZE)
            .numberOfElements(
                Math.min(
                    EntityService.SEARCH_RESULT_PAGE_SIZE,
                    hits.getSearchHits().size()
                )
            ).totalPages(
                (int) hits.getTotalHits() / EntityService.SEARCH_RESULT_PAGE_SIZE + 1 // TODO
            ).build();
    }

    /**
     * extract a simple map of terms and their counts from
     * a bucketed aggregation's buckets.
     */
    private Map<String, Long> getFacetsFromBuckets(Terms agg) {
        return ((Terms) agg).getBuckets().stream().collect(
            Collectors.toMap(
                Terms.Bucket::getKeyAsString,
                Terms.Bucket::getDocCount
            )
        );
    }

    /**
     * Converts terms aggregations to a map of field value counts.
     */
    public Map<String, Map<String, Long>> facets(SearchHits<?> hits) {
        if (hits.hasAggregations()) {
            Map<String, Map<String, Long>> facets = new HashMap<>();
            for (Aggregation agg : hits.getAggregations().asList()) {
                if (agg instanceof Terms) {
                    facets.put(
                        agg.getName(),
                        getFacetsFromBuckets((Terms) agg)
                    );
                } else if (agg instanceof Filter) {
                    ((Filter) agg).getAggregations().asList().stream().filter(
                        sub -> sub instanceof Terms
                    ).forEach(
                        sub -> facets.put(
                            agg.getName(),
                            getFacetsFromBuckets((Terms) sub)
                        )
                    );
                }
            }
            return facets;
        } else {
            return null;
        }
    }

    /**
     * Extract DTO objects out of a list of searchresults of an entity type.
     */
    public List<D> hitsToDTO(SearchHits<T> hits) {
        return hits.getSearchHits().stream().map(
            hit -> modelMapper.map(
                hit.getContent(),
                getDtoClass()
            )
        ).collect(Collectors.toList());
    }

    /**
     * Takes an Elasticsearch search result and the original page information and search
     * command, and puts it all into a serializable container ready for return
     * to the requesting client.
     */
    public SearchResultsWrapper<D> wrapSearchResults(
        SearchHits<T> hits, Pageable pageable, SearchCommand<D> command
    ) throws Exception {
        return new SearchResultsWrapper<D>(
            hitsToDTO(hits),
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
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withFields(
                getAutoCompleteSupport().getResponseFields()
            )
            .withFilter(
                (type != null && !type.isBlank()) ? QueryBuilders.termQuery("type", type) :
                    QueryBuilders.boolQuery()
            ).withQuery(
                getAutoCompleteSupport().autocompleteQuery(term)
            ).withPageable(
                PageRequest.of(0, 60)
            ).build();
        return hitsToDTO(
            search(query)
        );
    }

    /**
     * take a search command and based on the type figure out which
     * {@link AbstractEntityQueryBuilder entitiy query builder} or {@link
     * AbstractEntityIDsQueryBuilder entity ID query builder} you can put together
     * with this.
     */
    protected abstract AbstractEntityQueryBuilder<?, ?> getEntityQueryBuilder(
        SearchCommand<?> search
    );

    public Optional<AbstractEntityQueryBuilder<?, ?>> findMatchingEntityQueryBuilder(
        SearchCommand<?> search, Class<? extends Indexable> target
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

    @SuppressWarnings("unchecked")
    public Optional<SearchResultsWrapper<D>> search(SearchCommand<? extends AbstractDto> command, Class<? extends Indexable> entityType, Pageable page) {
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

}
