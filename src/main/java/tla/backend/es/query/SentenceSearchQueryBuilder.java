package tla.backend.es.query;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.SentenceEntity;
import tla.backend.service.ModelClass;
import tla.domain.command.PassportSpec;

@Slf4j
@Getter
@ModelClass(SentenceEntity.class)
public class SentenceSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {

    public void setTokens(Collection<TokenSearchQueryBuilder> tokenQueries) {
        tokenQueries.forEach(
            query -> this.filter(
                QueryBuilders.nestedQuery(
                    "tokens",
                    query.getNativeRootQueryBuilder(),
                    ScoreMode.None
                )
            )
        );
    }

    public void setPassport(PassportSpec spec) {
        log.info("set sentence search passport specs");
        if (spec != null && !spec.isEmpty()) {
            log.info("spawn text search dependency");
            var textSearchQuery = new TextSearchQueryBuilder();
            textSearchQuery.setExpansion(true);
            textSearchQuery.setPassport(spec);
            this.dependsOn(
                textSearchQuery,
                this::setTextIds,
                query -> {
                    log.info("extract IDs aggregation");
                    return ((Terms) query.getResults().getAggregations().get("ids")).getBuckets().stream().map(
                        Terms.Bucket::getKeyAsString
                    ).collect(Collectors.toList());
                }
            );
        }
    }

    public void setTextIds(Collection<String> textIds) {
        if (textIds != null) {
            log.info("sentence query: receive {} text IDs", textIds.size());
            this.filter(
                QueryBuilders.termsQuery(
                    "context.textId",
                    textIds
                )
            );
        }
    }

}