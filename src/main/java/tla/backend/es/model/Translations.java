package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.modelmapper.AbstractConverter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import tla.domain.model.Language;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Translations {
	
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lang-analyzer.html
	
	@Singular("de")
	@Field(type = FieldType.Text, analyzer = "german")
	private List<String> de;

	@Singular("en")
	@Field(type = FieldType.Text, analyzer = "english")
	private List<String> en;

	@Singular("fr")
	@Field(type = FieldType.Text, analyzer = "french")
	private List<String> fr;
	
	@Singular("ar")
	@Field(type = FieldType.Text, analyzer = "arabic")
	private List<String> ar;
	
	@Singular("it")
	@Field(type = FieldType.Text, analyzer = "italian")
	private List<String> it;

	/**
	 * Default constructor used by deserialization via ES entity mapper
	 */
	public Translations() {
		this.de = Collections.emptyList();
		this.en = Collections.emptyList();
		this.fr = Collections.emptyList();
		this.ar = Collections.emptyList();
		this.it = Collections.emptyList();
	}

	/**
	 * Converts an instance to a Map with preserved key order.
	 */
	public SortedMap<Language, List<String>> toMap() {
		SortedMap<Language, List<String>> converted = new TreeMap<Language, List<String>>();
		for (Entry<Language, List<String>> e : 
			Map.of(
				Language.DE, this.getDe(),
				Language.EN, this.getEn(),
				Language.FR, this.getFr(),
				Language.AR, this.getAr(),
				Language.IT, this.getIt()
			).entrySet()
		) {
			if (!e.getValue().isEmpty()) {
				converted.put(e.getKey(), e.getValue());
			}
		}
		return converted;
	}

	/**
	 * ModelMapper converter converting Translations objects to the map structure used in DTO objects.
	 */
	public static class ToMapConverter extends AbstractConverter<Translations, SortedMap<Language, List<String>>> {
		@Override
		protected SortedMap<Language, List<String>> convert(Translations source) {
			return source.toMap();
		}
	}

}