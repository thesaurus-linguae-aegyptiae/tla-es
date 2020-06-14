package tla.backend.es.model.parts;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LemmaWord {

    @Field(type = FieldType.Text, analyzer = "hieroglyph_analyzer")
    private String glyphs;

    @Field(type = FieldType.Object)
    private Transcription transcription;

}