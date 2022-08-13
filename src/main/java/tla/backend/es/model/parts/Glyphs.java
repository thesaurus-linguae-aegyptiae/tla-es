package tla.backend.es.model.parts;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Glyphs {

    @Field(type = FieldType.Text, analyzer = "hieroglyph_analyzer", searchAnalyzer = "hieroglyph_analyzer")
    private String unicode;

   
    @JsonAlias({"mdc_compact"})
   @Field(type = FieldType.Text, analyzer = "hieroglyph_analyzer", searchAnalyzer = "hieroglyph_analyzer")
    private String mdcCompact;

}