package tla.backend.es.model;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Translations {
	
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lang-analyzer.html
	
	@JsonProperty("de")
	@Field(type = FieldType.Text, analyzer = "german", name = "de")
	private List<String> german;

	@JsonProperty("en")
	@Field(type = FieldType.Text, analyzer = "english", name = "en")
	private List<String> english;

	@JsonProperty("fr")
	@Field(type = FieldType.Text, analyzer = "french", name = "fr")
	private List<String> french;
	
	@JsonProperty("ar")
	@Field(type = FieldType.Text, analyzer = "arabic", name = "ar")
	private List<String> arabic;
	
	@JsonProperty("it")
	@Field(type = FieldType.Text, analyzer = "italian", name = "it")
	private List<String> italian;

}