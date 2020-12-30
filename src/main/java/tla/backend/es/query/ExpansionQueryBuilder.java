package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;

public interface ExpansionQueryBuilder extends TLAQueryBuilder {

    /**
     * If set to true, query is considered an expansion query, meaning that no paged results
     * are being fetched, and an ID aggregation is added instead.
     */
    public default void setExpansion(boolean expansion) {
        if (expansion) {
            this.aggregate(
                AggregationBuilders.terms("ids").field("id").size(100000).order(BucketOrder.key(true))
            );
        }
    }

    public boolean isExpansion();

    public default void setRootIds(String[] ids) {
        this.must(
            QueryBuilders.termsQuery("paths.id.keyword", ids)
        );
    }

    public String[] getRootIds();

}