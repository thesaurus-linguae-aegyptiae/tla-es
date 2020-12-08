package tla.backend.es.query;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import lombok.extern.slf4j.Slf4j;
import tla.domain.command.PassportSpec;

@Slf4j
public class ThsRefExpansionDependency extends TLAQueryBuilder.QueryDependency<PassportSpec> {

    public ThsRefExpansionDependency(
        TLAQueryBuilder query,
        Consumer<PassportSpec> blockedMethod,
        Function<TLAQueryBuilder, PassportSpec> blockingMethod
    ) {
        super(query, blockedMethod, blockingMethod);
    }

    public static ThsRefExpansionDependency of(
        PassportIncludingQueryBuilder waitingQuery,
        String passportKey,
        Collection<String> thsIds
    ) {
        ThsSearchQueryBuilder expansionQuery = new ThsSearchQueryBuilder();
        expansionQuery.setRootIds(thsIds.toArray(new String[]{}));
        expansionQuery.setExpansion(true);
        return new ThsRefExpansionDependency(
            expansionQuery,
            waitingQuery::setPassport,
            query -> {
                log.info("result aggregations: {}", query.getResults().getAggregations());
                var expanded = new PassportSpec();
                expanded.put(
                    passportKey,
                    PassportSpec.ThsRefPassportValue.of(
                        ((Terms) query.getResults().getAggregations().get("ids")).getBuckets().stream().map(
                            Terms.Bucket::getKeyAsString
                        ).collect(Collectors.toList()),
                        false
                    )
                );
                return expanded;
            }
        );
    }

}
