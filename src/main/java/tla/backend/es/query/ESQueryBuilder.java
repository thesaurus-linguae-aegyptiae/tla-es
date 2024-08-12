package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.idsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.domain.model.meta.AbstractBTSBaseClass;

/**
 * This is an abstract search command adapter.
 */
@Slf4j
@Getter
public abstract class ESQueryBuilder implements TLAQueryBuilder {

    /**
     * The desired DTO type in which search results ought to be sent.
     */
    private Class<? extends AbstractBTSBaseClass> dtoClass;

    private BoolQueryBuilder nativeRootQueryBuilder;
    private List<AbstractAggregationBuilder<?>> nativeAggregationBuilders;
    protected SortSpec sortSpec = SortSpec.DEFAULT;

    private List<TLAQueryBuilder.QueryDependency<?>> dependencies;

    private ESQueryResult<?> result;

    public ESQueryBuilder() {
        this.nativeRootQueryBuilder = boolQuery();
        this.nativeAggregationBuilders = new LinkedList<>();
        this.dependencies = new LinkedList<>();
    }
    static final Map<String, String> criterias; 
    static { criterias = new HashMap<String, String>();
	criterias.put("timeSpan.begin_asc", "timeSpan.end_asc");
	criterias.put("timeSpan.begin_desc","timeSpan.end_desc");
	criterias.put("timeSpan.end_asc",  "timeSpan.begin_asc");
	criterias.put("timeSpan.end_desc", "timeSpan.begin_desc");
	//TODO Sortierkriterien vom Frontend mappen und nicht direkt auf ES zugreifen lassen
	//TODO Mapping for other criterias  sortKey,attestedSentenceCounts
    };
    
    public void setTimeSpanCriterias(String criteria) {
    	if(criterias.containsKey(criteria)) {
    	this.sortSpec.addSortingByString(criterias.get(criteria));
    	}
    }
    
    /**
     * Put together an actual Elasticsearch query ready for execution.
     */
    public NativeSearchQuery buildNativeSearchQuery(Pageable page) {
        var qb = new NativeSearchQueryBuilder().withQuery(
            this.getNativeRootQueryBuilder()
        ).withPageable(
            page
        ).withTrackTotalHits(
            page.isPaged()
        ).withSorts(
           this.sortSpec.Sorting()
        );
        log.info("query: {}", this.getNativeRootQueryBuilder());
        this.getNativeAggregationBuilders().forEach(
            agg -> {
                log.info("add aggregation to query: {}", agg);
                qb.withAggregations(agg);
            }
        );
        return qb.build();
    }

    public ESQueryResult<?> setResult(ESQueryResult<?> result) {
        this.result = result;
        return result;
    }

    public void setDTOClass(Class<? extends AbstractBTSBaseClass> dtoClass) {
        log.info("set DTO of search command adapter: {}", dtoClass);
        this.dtoClass = dtoClass;
    }

    public void setId(String[] ids) {
        if (ids != null) {
            log.info("add {} IDs to query", ids.length);
            this.filter(
                idsQuery().addIds(ids)
            );
        }
    }

    public void setEditor(String name) {
        if (name != null) {
            this.must(
                boolQuery().should(
                    matchQuery("editors.author", name)
                ).should(
                    matchQuery("editors.contributors", name)
                )
            );
        }
    }

    public void setSort(String sort) {
        log.info("receive sort order: {}", sort);
        this.sortSpec = SortSpec.from(sort);
    }

}