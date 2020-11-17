package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.LemmaEntity;
import tla.domain.command.LemmaSearch;
import tla.domain.command.TypeSpec;
import tla.domain.model.Language;
import tla.domain.model.Script;

@Slf4j
public class SimpleLemmaEntityQueryBuilder extends AbstractEntityQueryBuilder<LemmaSearch, LemmaEntity> {

    public SimpleLemmaEntityQueryBuilder(LemmaSearch command, Class<LemmaEntity> target) {
        super(command, target);
    }

    @Override
    protected QueryBuilder getQuery() {
        log.info("SEARCH COMMAND: {}", tla.domain.util.IO.json(getCommand()));
        BoolQueryBuilder qb = boolQuery();
        if (getCommand().getTranscription() != null) {
           qb.must(matchQuery("name", getCommand().getTranscription()).operator(Operator.AND));
        }
        if (getCommand().getTranslation() != null) {
            qb.must(translationQuery());
        }
        if (getCommand().getScript() != null && getCommand().getScript().length > 0) {
            qb.filter(scriptFilter());
        }
        if (getCommand().getBibliography() != null && !getCommand().getBibliography().trim().isEmpty()) {
            qb.must(
                matchQuery("passport.bibliography.bibliographical_text_field", getCommand().getBibliography()).operator(Operator.AND)
            );
        }
        if (getCommand().getRoot() != null && !getCommand().getRoot().trim().isEmpty()) {
            qb.must(
                matchQuery("relations.root.name", getCommand().getRoot())
            );
        }
        if (getCommand().getWordClass() != null) {
            qb.must(wordClassQuery());
        }
        if (getCommand().getAnno() != null) {
            qb.must(annotationTypeQuery(getCommand()));
        }
        return qb;
    }

    @Override
    protected SortSpec getSortSpec() {
        SortSpec sort = SortSpec.from(getCommand());
        if (sort.field.equals("root")) {
            sort.field = "relations.root.name";
        }
        return sort;
    }

    @Override
    protected Collection<AbstractAggregationBuilder<?>> getAggregations() {
        List<AbstractAggregationBuilder<?>> aggs = new ArrayList<>();
        TypeSpec wc = getCommand().getWordClass();
        if (wc != null) {
            if (wc.getType() != null && !wc.getType().isBlank()) {
                if (wc.getSubtype() == null || wc.getSubtype().isBlank()) {
                    aggs.add(
                        AggregationBuilders.terms("wordClass.subtype").field("subtype")
                    );
                }
            } else {
                aggs.add(
                    AggregationBuilders.terms("wordClass.type").field("type")
                );
            }
        } else {
            aggs.add(
                AggregationBuilders.terms("wordClass.type").field("type")
            );
        }
        if (getCommand().getScript() == null || getCommand().getScript().length < 1) {
            aggs.add(
                AggregationBuilders.terms("script").script(
                    new org.elasticsearch.script.Script(
                        "if (doc['id'].value.startsWith('d')) {return 'demotic';} if (!doc['type'].value.equals('root')) {return 'hieratic';}"
                    )
                )
            );
        }
        TypeSpec anno = getCommand().getAnno();
        if (anno == null || anno.getType() == null || anno.getType().isBlank()) {
            aggs.add(
                AggregationBuilders.filter(
                    "anno.type",
                    termQuery("relations.contains.eclass", "BTSAnnotation")
                ).subAggregation(
                    AggregationBuilders.terms("subagg").script(
                        new org.elasticsearch.script.Script(
                            "return 'Lemma';"
                        )
                    )
                )
            );
        }
        log.info("add aggregations to query: {}", aggs.size());
        return aggs;
    }

    /**
     * language phase query fragment
     */
    private BoolQueryBuilder scriptFilter() {
        BoolQueryBuilder scriptFilter = boolQuery();
        List<Script> scripts = Arrays.asList(getCommand().getScript());
        if (!scripts.contains(Script.HIERATIC)) {
            if (scripts.contains(Script.DEMOTIC)) {
                scriptFilter.must(prefixQuery("id", "d"));
            }
        } else {
            if (!scripts.contains(Script.DEMOTIC)) {
                scriptFilter.mustNot(prefixQuery("id", "d"));
            }
        }
        return scriptFilter;
    }

    /**
     * translation query fragment
     */
    private BoolQueryBuilder translationQuery() {
        BoolQueryBuilder translationsQuery = boolQuery();
        if (getCommand().getTranslation().getLang() != null && getCommand().getTranslation().getLang().length > 0) {
            for (Language lang : getCommand().getTranslation().getLang()) {
                translationsQuery.should(
                    matchQuery(
                        String.format("translations.%s", lang),
                        getCommand().getTranslation().getText()
                    )
                );
            }
        }
        return translationsQuery;
    }

    /**
     * part of speech query fragment
     */
    private BoolQueryBuilder wordClassQuery() {
        BoolQueryBuilder query = boolQuery();
        TypeSpec wordClass = getCommand().getWordClass();
        if (wordClass.getType() != null) {
            if (wordClass.getType().equals("excl_names")) {
                query.mustNot(termQuery("type", "entity_name"));
            } else if (wordClass.getType().equals("any")) {
            } else if (!wordClass.getType().isBlank()) {
                query.must(termQuery("type", wordClass.getType()));
            }
        }
        if (wordClass.getSubtype() != null && !wordClass.getSubtype().isBlank()) {
            query.must(termQuery("subtype", wordClass.getSubtype()));
        }
        return query;
    }

    /**
     * Create query requiring lemma to have annotations.
     */
    private BoolQueryBuilder annotationTypeQuery(LemmaSearch command) {
        BoolQueryBuilder q = boolQuery();
        TypeSpec anno = getCommand().getAnno();
        if (anno.getType() != null) {
            if (!anno.getType().isBlank()) {
                q.must(
                    termQuery("relations.contains.eclass", "BTSAnnotation")
                );
            }
        }
        return q;
    }
    
}