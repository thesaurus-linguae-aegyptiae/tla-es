package tla.backend.service.search;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.index.query.MultiMatchQueryBuilder;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Getter
@Builder
public class AutoCompleteSupport {

    public static final String[] QUERY_FIELDS = {
        "id", "name", "name._2gram", "name._3gram"
    };
    public static final String[] FETCH_FIELDS = {
        "id", "name", "type", "subtype", "eclass"
    };

    public static AutoCompleteSupport DEFAULT = new AutoCompleteSupport();

    @Singular
    private Map<String, Float> queryFields;

    @Builder.Default
    private String[] responseFields = FETCH_FIELDS;

    public AutoCompleteSupport() {
        this.queryFields = Arrays.asList(QUERY_FIELDS).stream().collect(
            Collectors.<String, String, Float>toMap(
                String::toString,
                field -> { return Float.valueOf(1); }
            )
        );
        this.responseFields = FETCH_FIELDS;
    }

    public AutoCompleteSupport(Map<String, Float> queryFields, String[] responseFields) {
        this();
        if (queryFields != null && !queryFields.isEmpty()) {
            queryFields.forEach(
                (field, boost) -> {
                    this.queryFields.merge(
                        field,
                        boost,
                        Float::sum
                    );
                }
            );
        }
        if (responseFields != null) {
            this.responseFields = Stream.concat(
                Arrays.stream(
                    responseFields
                ),
                Arrays.stream(
                    this.responseFields
                )
            ).distinct().toArray(
                String[]::new
            );
        }
    }

    public MultiMatchQueryBuilder autocompleteQuery(String term) {
        MultiMatchQueryBuilder query = new MultiMatchQueryBuilder(term)
            .type(MultiMatchQueryBuilder.Type.BOOL_PREFIX);
        for (Entry<String, Float> e : this.queryFields.entrySet()) {
            query = query.field(
                e.getKey(),
                e.getValue()
            );
        }
        return query;
    }
 
}