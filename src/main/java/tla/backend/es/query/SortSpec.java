package tla.backend.es.query;

import java.util.Arrays;

import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Representation of search order specifications.
 */
@NoArgsConstructor
@AllArgsConstructor
public class SortSpec {

    public static final String DELIMITER = "_";
    /**
     * an empty sort specification instance, whose {@link #primary()} method just returns
     * a standard {@link ScoreSortBuilder}.
     */
    public static final SortSpec DEFAULT = new SortSpec();

    /**
     * name of field by whose value to order.
     */
    protected String field;
    /**
     * sort order (i.e. {@link SortOrder.ASC} or {@link SortOrder.DESC})
     */
    protected SortOrder order;

    /**
     * Create new sort spec configured for ascending order ({@link SortOrder.ASC}) on given field.
     */
    public SortSpec(String field) {
        this(field, SortOrder.ASC);
    }

    /**
     * Create a new sort specification instance with given field name and sort order (<code>"asc"</code>/<code>"desc"</code>).
     */
    public SortSpec(String field, String order) {
        this(
            field,
            order.toLowerCase().equals("desc") ? SortOrder.DESC : SortOrder.ASC
        );
    }

    /**
     * Create a sort spec instance from a string consisting of a field name, followed by an order specifier (asc/desc),
     * seperated by the delimiter character defined in {@link #DELIMITER}.
     */
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
        if (this.field != null) {
            return SortBuilders.fieldSort(this.field).order(this.order);
        } else {
            return SortBuilders.scoreSort();
        }
    }

    public SortBuilder<?> secondary() {
        return SortBuilders.fieldSort("id").order(this.order);
    }

}