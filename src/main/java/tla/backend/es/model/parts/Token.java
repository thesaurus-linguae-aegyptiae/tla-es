package tla.backend.es.model.parts;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Token {

    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text)
    @JsonAlias("name")
    private String label;

    @Field(type = FieldType.Object)
    private Lemmatization lemma;

    @Field(type = FieldType.Object)
    private Flexion flexion;

    @Field(type = FieldType.Text, analyzer = "hieroglyph_analyzer")
    private String glyphs;

    @Field(type = FieldType.Object)
    private Transcription transcription;

    @Field(type = FieldType.Object)
    private Translations translations;

    @Field(type = FieldType.Keyword)
    private List<String> flags;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flexion {

        @Field(type = FieldType.Text)
        private String verbal;

        @Field(type = FieldType.Long)
        private Long numeric;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Lemmatization {

        @Field(type = FieldType.Object)
        private PartOfSpeech pos;

        @Field(type = FieldType.Keyword)
        private String id;
    }

}