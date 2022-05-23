package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.regexpQuery;

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
            //this.must(matchQuery("words.transcription.unicode", transcription));
			this.must(regexpQuery("words.transcription.unicode", maskRegExTranscription(transcription)));
			// works with Unicode only?
        }
    }

    public String maskRegExTranscription(String transcription) {
        if (transcription != null) {
			transcription = transcription.trim(); // cut whitespaces

			// case insensitive search (ES analyzer also indexes lowercase)
			transcription = transcription.toLowerCase(); 
			transcription = transcription.replace("h\u0331", "\u1e96"); // no atomic char in uppercase, merge in lowercase manually

			// no atomic char in transliteration => atomic workarounds
			transcription = transcription.replace("i\u032f", "i"); 
			transcription = transcription.replace("u\u032f", "u"); 
			transcription = transcription.replace("\u0131\u0357", "\ua7bd");  // BTS, right half ring above
			transcription = transcription.replace("h\u032d", "\u0125"); 

			// Other Unicode mapping for special chars
			transcription = transcription.replace("i\u0357", "\ua7bd");  // right half ring above, variant
			transcription = transcription.replace("\u1ec9", "\ua7bd");   // Ifao yod
			transcription = transcription.replace("\u021d", "\ua723");   // Ifao alif
			transcription = transcription.replace("\u02bf", "\ua725");   // ayn
			
			// Maskieren (nicht ignorieren)
            transcription = transcription.replace(".", "\\."); 
            transcription = transcription.replace("-", "\\-"); 
            transcription = transcription.replace("+", "\\+"); 
						
            // reatment of "(  )" als Options-Marker
            // transcription = transcription.replace("(", ""); 
            transcription = transcription.replace(")", ")?"); // ### to do: abfangen, wenn Klammern nicht ordentlich öffnen/schließen															

            // ignorieren: query und ES-Indizierung
            transcription = transcription.replace("{", ""); 
            transcription = transcription.replace("}", ""); 
            transcription = transcription.replace("⸢", ""); 
            transcription = transcription.replace("⸣", ""); 
            transcription = transcription.replace("〈", ""); 
            transcription = transcription.replace("〉", ""); 
            transcription = transcription.replace("⸮", ""); 
            // "?", "[" , and "]" are part of allowed RegEx syntax
			
			// BTS wildcards (any sign)
            transcription = transcription.replace("§", "."); // "§" in legacyTLA 
            transcription = transcription.replace("*", "."); // "*" new in newTLA 
			
			// treatment of right end
			if (transcription.endsWith("$")) { // "$": wirkliches String-Ende
				transcription = transcription.replace("$", ""); // remove "$" (all, just to be sure)
			} else {
				transcription = transcription + ".*"; // rechts beliebiger Anschluss
			}
        }
		return transcription;
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
            //this.must(matchQuery("relations.root.name", transcription));
            this.must(regexpQuery("relations.root.name", maskRegExTranscription(transcription)));
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
		// nicht einheitlich/elegant gel?st: die anderen vier Suchf?lle sind so gel?st, 
		// dass in der URL der Name der Variablen erscheint ("sortKey" bzw. "timeSpan.begin", 
		// gefolgt von "_asc"/"_desc", aber nicht hier bei "relations.root.name"
		// die Angaben in der URL sollten generell sprechend sein "transliteration_asc", ..., "timespan_asc", ...
		// und hier decodiert werden:
        //if (sortSpec.field.equals("transliteration")) {
        //    sortSpec.field = "sortKey";
        //}
		//...
        //if (sortSpec.field.equals("timespan_begin")) {
        //    sortSpec.field = "timeSpan.begin";
        //}
    }

}