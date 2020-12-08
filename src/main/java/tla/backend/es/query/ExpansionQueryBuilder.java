package tla.backend.es.query;

import org.elasticsearch.search.aggregations.AggregationBuilders;

public interface ExpansionQueryBuilder extends TLAQueryBuilder {

    /**
     * If set to true, query is considered an expansion query, meaning that no paged results
     * are being fetched, and an ID aggregation is added instead.
     */
    public default void setExpansion(boolean expansion) {
        if (expansion) {
            this.aggregate(
                AggregationBuilders.terms("ids").field("id").size(100000)
            );
        }
    }

    public boolean isExpansion();

    public void setRootIds(String[] ids);

    public String[] getRootIds();

}