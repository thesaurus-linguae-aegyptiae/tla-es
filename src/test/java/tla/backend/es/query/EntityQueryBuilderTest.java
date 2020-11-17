package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;

import tla.backend.es.model.ThsEntryEntity;
import tla.domain.command.OccurrenceSearch;
import tla.domain.command.TextSearch;

public class EntityQueryBuilderTest {

    private class MoreSpecificThsEntryIDQuery extends AbstractEntityIDsQueryBuilder<TextSearch, ThsEntryEntity> {

        protected MoreSpecificThsEntryIDQuery(TextSearch command) {
            super(command, ThsEntryEntity.class);
        }

        @Override
        protected QueryBuilder getQuery() {
            BoolQueryBuilder qb = boolQuery();
            if (getCommand().getDateId() != null) {
                qb.must(
                    termQuery("paths.id", getCommand().getDateId())
                );
            }
            return qb;
        }

    }

    @Test
    void testIdsQueryBuilder() {
        TextIdsQueryBuilder qb = new TextIdsQueryBuilder(new OccurrenceSearch());
        Query q = qb.build(Pageable.unpaged());
        assertAll("built query must have IDs aggregation",
            () -> assertTrue(
                ((NativeSearchQuery) q).getAggregations().stream().anyMatch(
                    agg -> agg.getName().equals("ids")
                ),
                "query has an aggregation 'ids'"
            )
        );
        TextSearch form = new TextSearch();
        form.setDateId("T5G5CJPBKBGW3DAGCAUTS35JRY");
        AbstractEntityIDsQueryBuilder<TextSearch, ThsEntryEntity> eqb = new MoreSpecificThsEntryIDQuery(form);
        NativeSearchQuery nsq = eqb.build();
        assertAll("see if entity IDs query matches expectations",
            () -> assertEquals(BoolQueryBuilder.class, nsq.getQuery().getClass(), "should be bool query"),
            () -> assertTrue(
                nsq.getAggregations().stream().anyMatch(
                    agg -> agg.getName().equals("ids")
                ),
                "should have 'ids' aggregation"
            ),
            () -> assertEquals(1, nsq.getAggregations().size(), "should have 1 aggregation")
        );
    }

}