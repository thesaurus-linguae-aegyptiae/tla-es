package tla.backend.es.query;

import org.elasticsearch.index.query.QueryBuilders;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.domain.command.PassportSpec;

@Slf4j
@Getter
public class PassportIncludingQueryBuilder extends ESQueryBuilder {

    private PassportSpec passport = new PassportSpec();

    private void processThsReferences(String key, PassportSpec.ThsRefPassportValue values) {
        if (values.isExpand()) {
            this.dependsOn(
                ThsRefExpansionDependency.of(
                    this, key, values.getValues()
                )
            );
            this.getPassport().put(key, values);
        } else {
            this.filter(
                QueryBuilders.termsQuery(
                    String.format("passport.%s.id.keyword", key),
                    this.getPassport().put(key, values).getValues()
                )
            );
        }
    }

    public void setPassport(PassportSpec spec) {
        if (spec != null && !spec.isEmpty()) {
            log.info("passport specs: {}", tla.domain.util.IO.json(spec));
            spec.entrySet().forEach(
                e -> {
                    if (!e.getValue().getValues().isEmpty()) {
                        if (e.getValue() instanceof PassportSpec.ThsRefPassportValue) {
                            processThsReferences(e.getKey(), (PassportSpec.ThsRefPassportValue) e.getValue());
                        } else {
                            this.must(
                                QueryBuilders.matchQuery(
                                    String.format("passport.%s", e.getKey()),
                                    String.join(" ", e.getValue().getValues())
                                )
                            );
                        }
                    }
                }
            );
        }
    }

}