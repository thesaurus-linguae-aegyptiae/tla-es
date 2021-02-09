package tla.backend.es.query;

import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tla.backend.es.model.meta.Indexable;
import tla.domain.dto.extern.PageInfo;

/**
 * ES search hits container with paging information.
 */
@Getter
@Setter
@NoArgsConstructor
public class ESQueryResult<T extends Indexable> {

    /**
     * How many search results fit in one single page.
     */
    public static final int SEARCH_RESULT_PAGE_SIZE = 20;

    private SearchHits<T> hits;

    private PageInfo pageInfo;

    public ESQueryResult(SearchHits<T> hits, Pageable page) {
        this.hits = hits;
        this.pageInfo = page.isUnpaged() ? null : pageInfo(hits, page);
    }

    /**
     * if there is an IDs aggregation, extract IDs from it.
     * @return
     */
    public List<String> getIDAggValues() {
        if (this.hits.getAggregations() != null && this.hits.getAggregations().get("ids") != null) {
            return (
                (Terms) this.hits.getAggregations().get("ids")
            ).getBuckets().stream().map(
                Terms.Bucket::getKeyAsString
            ).collect(
                Collectors.toList()
            );
        }
        return null;
    }

    /**
     * Create a serializable transfer object containing page information
     * about an ES search result.
     */
    public static PageInfo pageInfo(SearchHits<?> hits, Pageable pageable) {
        return PageInfo.builder()
            .number(pageable.getPageNumber())
            .totalElements(hits.getTotalHits())
            .size(SEARCH_RESULT_PAGE_SIZE)
            .numberOfElements(
                Math.min(
                    SEARCH_RESULT_PAGE_SIZE,
                    hits.getSearchHits().size()
                )
            ).totalPages(
                (int) hits.getTotalHits() / SEARCH_RESULT_PAGE_SIZE + 1 // TODO
            ).build();
    }

}