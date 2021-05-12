package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;

public interface ExpansionQueryBuilder extends TLAQueryBuilder {

    final static String ID_FIELD = "id";
    final static int ID_AGG_SIZE = 1000000;

    /**
     * If set to true, query is considered an expansion query, meaning that no paged results
     * are being fetched, and an ID aggregation is added instead.
     */
    public default void setExpansion(boolean expansion) {
        if (expansion) {
            this.aggregate(
                AggregationBuilders.terms(
                    ESQueryResult.AGGS_ID_IDS
                ).field(ID_FIELD).size(ID_AGG_SIZE).order(
                    BucketOrder.key(true)
                )
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