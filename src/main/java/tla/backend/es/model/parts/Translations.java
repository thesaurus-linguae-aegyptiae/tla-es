package tla.backend.es.model.parts;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.modelmapper.AbstractConverter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import tla.domain.model.Language;

@Data
@Slf4j
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
	 * Retrieves translation(s) for a given language.
	 */
	@SuppressWarnings("unchecked")
	public List<String> get(Language lang) {
		String getterName = String.format(
			"get%s",
			lang.toString().toUpperCase().charAt(0) + lang.toString().substring(1)
		);
		try {
			Object res = Translations.class.getMethod(getterName).invoke(this);
			if (res != null && res instanceof List) {
				return (List<String>) res;
			}
		} catch (Exception e) {
			log.error(
				String.format(
					"Could not access translations in %s via method %s on %s!",
					lang.toString(),
					getterName,
					this
				),
				e
			);
		}
		return null;
	}

	/**
	 * Converts an instance to a Map with preserved key order.
	 */
	public SortedMap<Language, List<String>> toMap() {
		SortedMap<Language, List<String>> converted = new TreeMap<Language, List<String>>();
		for (Language lang : Language.values()) {
			try {
				List<String> langTranslations = this.get(lang);
				if (langTranslations != null) {
					converted.put(
						lang,
						(List<String>) langTranslations
					);
				}
			} catch (Exception e) {
				log.error(
					String.format(
						"Could not map translations %s",
						this
					),
					e
				);
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
			return source != null ? source.toMap() : null;
		}
	}

}