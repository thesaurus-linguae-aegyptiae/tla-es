package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;

import lombok.Getter;
import tla.backend.es.model.LemmaEntity;
import tla.backend.service.ModelClass;
import tla.domain.command.TypeSpec;
import tla.domain.model.Script;

@Getter
@ModelClass(LemmaEntity.class)
public class LemmaSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {

    static List<FacetSpec> facetSpecs = List.of(
        FacetSpec.field("wordClass.type", "type"),
        FacetSpec.field("wordClass.subtype", "subtype"),
        FacetSpec.script(
            "script",
            "if (doc['id'].value.startsWith('d')) {return 'demotic';} if (!doc['type'].value.equals('root')) {return 'hieratic';}"
        )
    );

    public void setScript(List<Script> scripts) {
        var scriptFilter = boolQuery();
        if (scripts != null) {
            if (!scripts.contains(Script.HIERATIC)) {
                if (scripts.contains(Script.DEMOTIC)) {
                    scriptFilter.must(prefixQuery("id", "d"));
                }
            } else {
                if (!scripts.contains(Script.DEMOTIC)) {
                    scriptFilter.mustNot(prefixQuery("id", "d"));
                }
            }
        }
        this.filter(scriptFilter);
    }

    public void setTranscription(String transcription) {
        if (transcription != null) {
            this.must(matchQuery("words.transcription.unicode", transcription));
        }
    }

    public void setWordClass(TypeSpec wordClass) {
        BoolQueryBuilder query = boolQuery();
        if (wordClass != null) {
            if (wordClass.getType() != null) {
                if (wordClass.getType().equals("excl_namestitlesepithets")) { // TODO
                    query.mustNot(termQuery("type", "entity_name"));
                    query.mustNot(termQuery("type", "epitheton_title"));
                } else if (wordClass.getType().equals("excl_names")) { // TODO
                    query.mustNot(termQuery("type", "entity_name"));
                } else if (wordClass.getType().equals("any")) {
                } else if (!wordClass.getType().isBlank()) {
                    query.must(termQuery("type", wordClass.getType()));
                }
            }
            if (wordClass.getSubtype() != null) {
                query.must(termQuery("subtype", wordClass.getSubtype()));
            }
        }
        this.must(query);
    }

    public void setRoot(String transcription) { // TODO spawn join query
        if (transcription != null) {
            this.must(matchQuery("relations.root.name", transcription));
        }
    }

    public void setAnno(TypeSpec annotationType) { // TODO spawn join query
        BoolQueryBuilder q = boolQuery();
        if (annotationType != null) {
            if (annotationType.getType() != null) {
                if (!annotationType.getType().isBlank()) {
                    q.must(
                        termQuery("relations.contains.eclass", "BTSAnnotation")
                    );
                }
            }
        }
        this.must(q);
    }

    public void setBibliography(String bibliography) {
        if (bibliography != null) {
            this.must(
                matchQuery(
                    "passport.bibliography.bibliographical_text_field",
                    bibliography
                ).operator(Operator.AND)
            );
        }
    }

    public void setSort(String sort) {
        super.setSort(sort);
        if (sortSpec.field.equals("root")) {
            sortSpec.field = "relations.root.name";
        }
    }

}