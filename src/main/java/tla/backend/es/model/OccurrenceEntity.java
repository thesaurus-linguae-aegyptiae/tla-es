package tla.backend.es.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "occurrence", type = "occurrence")
public class OccurrenceEntity {

    @Id
    @NonNull
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Object)
    private Flexion flexion;

    @Field(type = FieldType.Object)
    private Lemmatization lemma;

    @Field(type = FieldType.Object)
    private OccurrenceLocation location;

    @Field(type = FieldType.Object)
    private Transcription transcription;

    @Field(type = FieldType.Object)
    private Translations translations;

    @Data
    public static class Flexion {
        private String glossing;
        @Field(type = FieldType.Keyword)
        private Integer numeric;
        private String verbal;
    }

    @Data
    public static class Lemmatization {
        @Field(type = FieldType.Keyword)
        private String id;
        private String pos;
    }

    @Data
    public static class OccurrenceLocation {

        private String line;
        private String para;
        private Integer position;

        @JsonAlias("sentence")
        @Field(type = FieldType.Keyword)
        private String sentenceId;

        @JsonAlias("text")
        @Field(type = FieldType.Keyword)
        private String textId;

        @JsonAlias("token")
        @Field(type = FieldType.Keyword)
        private String tokenId;
    }
}
