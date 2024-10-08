package tla.backend.es.query;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.regexpQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.search.sort.SortOrder;

import lombok.Getter;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.query.SortSpec.FieldOrder;
import tla.backend.service.ModelClass;
import tla.domain.command.TypeSpec;
import tla.domain.command.TranscriptionSpec;
//import tla.domain.command.RootSpec;
import tla.domain.model.Script;

@Getter
@ModelClass(LemmaEntity.class)
public class LemmaSearchQueryBuilder extends ESQueryBuilder implements MultiLingQueryBuilder {
	static List<FacetSpec> facetSpecs = List.of(FacetSpec.field("wordClass.type", "type"),
			FacetSpec.field("wordClass.subtype", "subtype"), FacetSpec.script("script",
					"if (doc['id'].value.startsWith('d')) {return 'demotic';} if (!doc['type'].value.equals('root')) {return 'hieratic';}"));

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

	public void setTranscription(TranscriptionSpec transcription) {
		if (transcription.getText() != null) {
			if (transcription.getEnc()[0].equals("mdc")) {
				this.must(regexpQuery("transcription.mdc", maskRegExTranscription(transcription.getText())));
			} else {
				this.must(regexpQuery("transcription.unicode",
						maskRegExTranscription(normalizeUnicode(transcription.getText()))));

			}
			// works with Unicode only?
		}

	}

	/*
	 * public void setRoot(RootSpec root) {
	 * 
	 * if (root != null) { //int posEnc=transcription.indexOf("|"); //String
	 * encod=transcription.substring(posEnc+1);
	 * 
	 * if (root.getEnc()[0].equals("mdc")){
	 * 
	 * this.must(regexpQuery("relations.root.name",
	 * maskRegExTranscription(root.getText()))); } else
	 * this.must(regexpQuery("relations.root.name",
	 * maskRegExTranscription(root.getText()))); // works with Unicode only? }
	 * 
	 * }
	 */

	public String normalizeUnicode(String transcription) {
		if (transcription != null) {
			// case insensitive search (ES analyzer also indexes lowercase)
			transcription = transcription.toLowerCase();
			transcription = transcription.replace("h\u0331", "ẖ"); // no atomic char as capital, now lowercase
			transcription = transcription.replace("i\u0357", "\u0131\u0357"); // BTS yod capital, now lowercase

			// Other Unicode mapping for special chars; normalization
			transcription = transcription.replace("d\u0331", "ḏ"); // non-atomic encodings
			transcription = transcription.replace("t\u0331", "ṯ");
			transcription = transcription.replace("h\u0323", "ḥ");
			transcription = transcription.replace("t\u0323", "ṭ");
			transcription = transcription.replace("k\u0323", "ḳ");

			transcription = transcription.replace("ṭ", "d"); // Schenkel's transliteration
			transcription = transcription.replace("č\u0323", "ḏ");
			transcription = transcription.replace("č", "ṯ");

			transcription = transcription.replace("ś", "s"); // traditional s
			transcription = transcription.replace("ḳ", "q"); // traditional qaf

			transcription = transcription.replace("⸗", "="); // double oblique hyphen
			transcription = transcription.replace("〈", "〈"); // U+27E8 => U+2329, BTS, obsolet
			transcription = transcription.replace("〉", "〉");
			transcription = transcription.replace("⌈", "⸢"); // U+2308, obsolet => U+2E22
			transcription = transcription.replace("⌉", "⸣");

			transcription = transcription.replace("\u1ec9", "\u0131\u0357"); // Ifao yod => BTS yod
			transcription = transcription.replace("i\u0486", "\u0131\u0357"); // yod with psili pneumata
			transcription = transcription.replace("i\u0313", "\u0131\u0357"); // yod with superscript comma
			transcription = transcription.replace("\u021d", "\ua723"); // Ifao alif
			transcription = transcription.replace("\u02bf", "\ua725"); // ayn workaround

			// no atomic char in transliteration => atomic workarounds
			transcription = transcription.replace("i\u032f", "i"); // atomic workaround for ult.-inf.-i
			transcription = transcription.replace("u\u032f", "u"); // atomic workaround for ult.-inf.-u
			transcription = transcription.replace("\u0131\u0357", "\ua7bd"); // BTS yod => Egyptological Yod
			transcription = transcription.replace("h\u032d", "\u0125"); // atomic workaround for demotic h
		}
		return transcription;
	}

	public String maskRegExTranscription(String transcription) {
		if (transcription != null) {
			transcription = transcription.trim(); // cut whitespaces
			transcription = transcription.replaceAll("\\s+", " ");

			// Maskieren (nicht ignorieren)
			transcription = transcription.replace(".", "\\.");
			transcription = transcription.replace("-", "\\-");
			transcription = transcription.replace("+", "\\+");

			// treatment of "( )" als Options-Marker
			// transcription = transcription.replace("(", "");
			transcription = transcription.replace(")", ")?"); // ### to do: abfangen, wenn Klammern nicht ordentlich
																// öffnen/schließen

			// ignorieren: query und ES-Indizierung
			transcription = transcription.replace("{", "");
			transcription = transcription.replace("}", "");
			transcription = transcription.replace("⸢", ""); // U+2E22
			transcription = transcription.replace("⸣", "");
			transcription = transcription.replace("〈", ""); // U+2329, BTS, obsolet
			transcription = transcription.replace("〉", "");
			transcription = transcription.replace("⸮", "");
			// "?", "[" , and "]" are part of allowed RegEx syntax

			// leagacy TLA wildcards
			transcription = transcription.replace("§", "."); // "§" in legacyTLA

			// Usual wildcards
			transcription = transcription.replace("_", "."); // any sigle sign
			transcription = transcription.replace("*", ".*"); // zero or more signs

			// treatment of right end
			if (transcription.endsWith("$")) { // "$": wirkliches String-Ende
				transcription = transcription.replace("$", ""); // remove "$" (all, just to be sure)
			} else if (!transcription.endsWith(".*")) { // redundant if already ends with ".*"
				transcription = transcription + ".*"; // right: any signs may follow
			}

			// treatment of left end
			if (transcription.startsWith(">")) { // ">": search at beginning or in the middle of lemma transliteration
				transcription = transcription.replace(">", ""); // remove "^" (all, just to be sure)
				// find words in the middle too
				transcription = "(.+[\\- ])?" + transcription; // left: anything at beginnig of lemma or after "-" or
																// blank
			} else if (transcription.startsWith("\\-")) { // "-": search of non-first lemma
				transcription = "(.+)?" + transcription; // left: anything may occur before the '-'
			}
		}
		return transcription;
	}

	public void setWordClass(TypeSpec wordClass) {
		BoolQueryBuilder query = boolQuery();
		if (wordClass != null) {
			if (wordClass.getType() != null) {
				if (wordClass.getType().equals("excl_namestitlesepithets")) {
					query.mustNot(termQuery("type", "entity_name"));
					query.mustNot(termQuery("type", "epitheton_title"));
				} else if (wordClass.getType().equals("excl_names")) {
					query.mustNot(termQuery("subtype", "person_name"));
					query.mustNot(termQuery("subtype", "kings_name"));
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
			// this.must(matchQuery("relations.root.name", transcription));
			this.must(regexpQuery("relations.root.name", maskRegExTranscription(transcription)));
		}
	}

	public void setAnno(TypeSpec annotationType) { // TODO spawn join query
		BoolQueryBuilder q = boolQuery();
		if (annotationType != null) {
			if (annotationType.getType() != null) {
				if (!annotationType.getType().isBlank()) {
					q.must(termQuery("relations.contains.eclass", "BTSAnnotation"));
				}
			}
		}
		this.must(q);
	}

	public void setBibliography(String bibliography) {
		if (bibliography != null) {
			this.must(matchQuery("passport.bibliography.bibliographical_text_field", bibliography)
					.operator(Operator.AND));
		}
	}

	public void setSort(String sort) {
		super.setSort(sort);
		setTimeSpanCriterias(sort);
		// Always the final criteria
		sortSpec.addSortingByString("sortKey_asc");
	}
}
