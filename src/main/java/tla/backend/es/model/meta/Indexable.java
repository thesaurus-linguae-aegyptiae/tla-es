package tla.backend.es.model.meta;


/**
 * Using this interface only makes sense if the implementation is annotated with
 * {@link org.springframework.data.elasticsearch.annotations.Document}
 */
public interface Indexable {

    public String getId();

}