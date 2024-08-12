package tla.backend.es.query;

import java.util.Arrays;
import java.util.ArrayList;

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
public class SortSpec {

	public static final String DELIMITER = "_";
	/**
	 * an empty sort specification instance, whose {@link #primary()} method just
	 * returns a standard {@link ScoreSortBuilder}.
	 */
	public static final SortSpec DEFAULT = new SortSpec();

	protected ArrayList<FieldOrder> FieldOrders = new ArrayList<FieldOrder>();

	/**
	 * Create new sort spec configured for ascending order ({@link SortOrder.ASC})
	 * on given field.
	 */
	public SortSpec(FieldOrder FieldOrder) {
		this.FieldOrders.add(FieldOrder);
	}

	public SortSpec(ArrayList<FieldOrder> FieldOrders) {
		this.FieldOrders = FieldOrders;
	}

	public void addFieldOrder(FieldOrder FieldOrder) {
		this.FieldOrders.add(FieldOrder);
	}
	
	public void addSortingByString(String criteria) {
		this.addFieldOrder(SortSpec.from(criteria).FieldOrders.get(0));
	}

	public static class FieldOrder {
		/**
		 * name of field by whose value to order.
		 */
		protected String field;
		/**
		 * sort order (i.e. {@link SortOrder.ASC} or {@link SortOrder.DESC})
		 */
		protected SortOrder order;

		public FieldOrder(String field, SortOrder order) {
			this.field = field;
			this.order = order;
		}
	}

	/**
	 * Create a sort spec instance from a string consisting of a field name,
	 * followed by an order specifier (asc/desc), seperated by the delimiter
	 * character defined in {@link #DELIMITER}.
	 */
	public static SortSpec from(String source) {

		if (source != null) {
			String[] segm = source.split(DELIMITER);
			String field = String.join(DELIMITER, Arrays.asList(segm).subList(0, segm.length - 1));
			if (segm.length > 1) {
				if (segm[segm.length - 1].equals("asc")) {
					return new SortSpec(new FieldOrder(field, SortOrder.ASC));
				} else {
					return new SortSpec(new FieldOrder(field, SortOrder.DESC));
				}
			} else {
				return new SortSpec(new FieldOrder(segm[0], SortOrder.ASC));
			}
		} else {
			return new SortSpec(new FieldOrder("id", SortOrder.ASC));
		}
	}

	public ArrayList<SortBuilder<?>> Sorting() {
		ArrayList<SortBuilder<?>> allSortBuilders = new ArrayList<SortBuilder<?>>();
		for (FieldOrder fieldOrder : FieldOrders) {
			allSortBuilders.add(SortBuilders.fieldSort(fieldOrder.field).order(fieldOrder.order));
		}
		return allSortBuilders;
	}
}