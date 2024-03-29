package tla.backend.es.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import lombok.Getter;
import tla.backend.es.model.meta.Indexable;
import tla.domain.dto.extern.PageInfo;

/**
 * ES search hits container with paging information.
 *
 * <code>&lt;T&gt;</code>: an {@link Indexable} entity class
 */
@Getter
public class ESQueryResult<T extends Indexable> {

	/**
	 * How many search results fit in one single page by default.
	 */
	public static final int SEARCH_RESULT_PAGE_SIZE = 10;

	/**
	 * IDs aggregation identifier
	 */
	public static final String AGGS_ID_IDS = "ids";

	private Map<String, Map<String, Long>> aggregations;

	private SearchHits<T> hits;

	private PageInfo pageInfo;

	public ESQueryResult() {
		this.aggregations = new HashMap<>();
	}

	public ESQueryResult(SearchHits<T> hits, Pageable page) {
		this();
		this.hits = hits;
		this.pageInfo = page.isUnpaged() ? null : pageInfo(hits, page);
	}

	/**
	 * if there is an IDs aggregation, extract IDs from it.
	 */
	public Collection<String> getIDAggValues() {
		return this.getAggregation(AGGS_ID_IDS).keySet();
	}

	/**
	 * extract a terms aggregation of the specified name.
	 *
	 * @return map of aggregated terms and corresponding document counts, or an
	 *         empty map if the aggregation doesn't exist
	 */
	public Map<String, Long> getAggregation(String agg) {
		return this.aggregations.getOrDefault(agg, this.getAggregationFromESHits(agg));
	}

	private Map<String, Long> getAggregationFromESHits(String agg) {
		Aggregations aggregations = (Aggregations) this.hits.getAggregations().aggregations();
		if (aggregations == null || aggregations.get(agg) == null) {
			return Collections.emptyMap();
		}
		return ((Terms) aggregations.get(agg)).getBuckets().stream()
				.collect(Collectors.toMap(Terms.Bucket::getKeyAsString, Terms.Bucket::getDocCount));
	}

	/**
	 * save terms aggregation results.
	 */
	public void addAggregationResults(Map<String, Map<String, Long>> aggs) {
		this.aggregations.putAll(aggs);
	}

	/**
	 * return total number of results.
	 */
	public long getHitCount() {
		Set<String> idSet = new HashSet<>();
		int index;
		Iterator<SearchHit<T>> iterator = hits.iterator();
		while (iterator.hasNext()) {
			SearchHit searchHit = (SearchHit) iterator.next();
			String source = searchHit.getId();
			index = source.indexOf("-");
			if (index != -1) {
				source = source.substring(0, index);
			}
			idSet.add(source);
		}
		return (long) idSet.size();
	}

	/**
	 * Create a serializable transfer object containing page information about an ES
	 * search result.
	 */
	public static PageInfo pageInfo(SearchHits<?> hits, Pageable pageable) {
		return PageInfo.builder().number(pageable.getPageNumber()).totalElements(hits.getTotalHits())
				.size(pageable.getPageSize())
				.numberOfElements(Math.min(pageable.getPageSize(), hits.getSearchHits().size()))
				.totalPages((int) hits.getTotalHits() / pageable.getPageSize()
						+ (hits.getTotalHits() % pageable.getPageSize() < 1 ? 0 : 1))
				.build();
	}

}
