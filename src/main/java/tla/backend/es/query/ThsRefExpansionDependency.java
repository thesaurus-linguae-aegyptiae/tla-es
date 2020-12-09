package tla.backend.es.query;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

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
                log.info("result aggregations: {}", query.getResult().getHits().getAggregations());
                var expanded = new PassportSpec();
                expanded.put(
                    passportKey,
                    PassportSpec.ThsRefPassportValue.of(
                        query.getResult().getIDAggValues(),
                        false
                    )
                );
                return expanded;
            }
        );
    }

}
