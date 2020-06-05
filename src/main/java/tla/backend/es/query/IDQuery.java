package tla.backend.es.query;

import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;

/**
 * This interface is to be implemented by {@link AbstractEntityQueryBuilder} subclasses
 * to indicate that the request body (query) they build declares an aggregation "<code>ids</code>"
 * containing the IDs of all matching documents.
 */
public interface IDQuery {

    /**
     * build a query, but with an extra aggregation "<code>ids</code>".
     */
    public NativeSearchQuery build();

}