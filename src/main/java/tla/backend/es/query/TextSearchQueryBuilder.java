package tla.backend.es.query;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.TextEntity;
import tla.backend.service.ModelClass;

@Slf4j
@Getter
@ModelClass(TextEntity.class)
public class TextSearchQueryBuilder extends PassportIncludingQueryBuilder implements ExpansionQueryBuilder {

    public static final String AGG_ID_DATE = "passport.date.date.date";

    private boolean expansion;

    private String[] rootIds;

    public TextSearchQueryBuilder() {
        this.aggregate(
            AggregationBuilders.terms(AGG_ID_DATE).field(
                String.format("%s.id.keyword", AGG_ID_DATE)
            ).size(1000).order(
                BucketOrder.key(true)
            )
        );
    }

    @Override
    public void setExpansion(boolean expansion) {
        log.info("text query: set IDs aggregation");
        ExpansionQueryBuilder.super.setExpansion(expansion);
        this.expansion = expansion;
    }

    @Override
    public void setRootIds(String[] ids) {
        // TODO Auto-generated method stub
    }

}
