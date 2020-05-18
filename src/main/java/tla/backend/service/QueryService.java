package tla.backend.service;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.meta.BaseEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.model.meta.TLAEntity;
import tla.domain.dto.DocumentDto;
import tla.domain.dto.extern.SingleDocumentWrapper;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.BTSeClass;

/**
 * Implementing subclasses must be annotated with {@link ModelClass} and be instantiated
 * using the no-args default constructor.
 * They should also be annotated with {@link Service} for component scanning / dependency injection.
 */
@Slf4j
public abstract class QueryService<T extends Indexable> {

    @Autowired
    private RestHighLevelClient restClient;

    protected static Map<Class<? extends Indexable>, QueryService<? extends Indexable>> modelClassServices = new HashMap<>();

    /**
     * Default constructor registering services under the eclass specified via a {@link BTSeClass}
     * annotation.
     */
    protected QueryService() {
        for (Annotation a : this.getClass().getAnnotations()) {
            log.info("annotation on clas {}: {}", this.getClass().getName(), a.getClass().getName());
            if (a instanceof ModelClass) {
                log.info("register model class service {}", this.getClass().getName());
                modelClassServices.put(
                    ((ModelClass) a).value(),
                    this
                );
            }
        }
        log.info("number of registered services: {}", modelClassServices.size());
    }

    /**
     * Returns the query service registered for a given model class, or null if no such model class have been
     * registered.
     * Registration takes place at construction time of any service with a {@link ModelClass} annotation.
     */
    public static QueryService<? extends Indexable> getService(Class<? extends Indexable> modelClass) {
        if (modelClassServices.containsKey(modelClass)) {
            return modelClassServices.get(modelClass);
        } else {
            log.error("No service registered for eclass '{}'!'", modelClass);
            return null;
        }
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
    public abstract ElasticsearchRepository<T, String> getRepo();

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
    public SingleDocumentWrapper<DocumentDto> getDetails(String id) {
        T document = this.retrieve(id);
        final SingleDocumentWrapper<DocumentDto> container;
        if (document != null) {
            container = new SingleDocumentWrapper<>(
                ModelConfig.toDTO(document)
            );
            Collection<BaseEntity> relatedObjects = new HashSet<>();
            relatedObjects.addAll(this.retrieveRelatedDocs(document));
            relatedObjects.addAll(this.retrieveReferencedThesaurusEntries(document));
            relatedObjects.forEach(
                relatedObject -> {
                    container.addRelated(
                        ModelConfig.toDTO(relatedObject)
                    );
                }
            );
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
    protected Collection<BaseEntity> retrieveReferencedObjects(Collection<ObjectReference> references) {
        return references.stream().map(
            ref -> {
                BaseEntity referencedEntity = this.retrieveSingleBTSDoc(
                    ref.getEclass(),
                    ref.getId()
                );
                if (referencedEntity == null) {
                    log.error(
                        "Could not retrieve referenced object {}",
                        ref
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
            Set<ObjectReference> previews = new HashSet<>();
            BaseEntity entity = (BaseEntity) document;
            entity.getRelations().entrySet().forEach(
                e -> {previews.addAll(e.getValue());}
            );
            return retrieveReferencedObjects(previews);
        }
        return Collections.emptyList();
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
            Set<ObjectReference> previews = new HashSet<>();
            TLAEntity entity = (TLAEntity) document;
            if (entity.getPassport() != null) {
                previews.addAll(entity.getPassport().extractObjectReferences());
            }
            return retrieveReferencedObjects(previews);
        }
        return Collections.emptyList();
    }

    /**
     * Execute an Elasticsearch query against the index defined for <code>entityClass</code>.
     */
    public SearchResponse query(
        Class<? extends Indexable> entityClass,
        QueryBuilder queryBuilder,
        AggregationBuilder aggsBuilder
    ) {
        String index = ModelConfig.getIndexName(entityClass);
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
        QueryService<? extends Indexable> service = modelClassServices.getOrDefault(modelClass, null);
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

}