package tla.backend.es.query;

import java.util.Arrays;

import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

/**
 * Representation of search order specifications.
 */
public class SortSpec {

    public static final String DELIMITER = "_";

    protected String field;
    protected SortOrder order;

    public SortSpec(String field) {
        this(field, SortOrder.ASC);
    }

    public SortSpec(String field, SortOrder order) {
        this.field = field;
        this.order = order;
    }

    public SortSpec(String field, String order) {
        this(
            field,
            order.toLowerCase().equals("desc") ? SortOrder.DESC : SortOrder.ASC
        );
    }

    public static SortSpec from(String source) {
        if (source != null) {
            String[] segm = source.split(DELIMITER);
            String field = String.join(
                DELIMITER,
                Arrays.asList(segm).subList(0, segm.length - 1)
            );
            if (segm.length > 1) {
                return new SortSpec(field, segm[segm.length - 1]);
            } else {
                return new SortSpec(segm[0]);
            }
        } else {
            return new SortSpec("id");
        }
    }

    public SortBuilder<?> primary() {
        return SortBuilders.fieldSort(this.field).order(this.order);
    }

    public SortBuilder<?> secondary() {
        return SortBuilders.fieldSort("id").order(this.order);
    }
}
