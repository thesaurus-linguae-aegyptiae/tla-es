package tla.backend.es.model.parts;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transcription {

    @Field(type = FieldType.Text)
    private String unicode;

    @Field(type = FieldType.Text)
    private String mdc;

    //TODO hieroglyphs?
}