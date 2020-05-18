package tla.backend.es.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.parts.Transcription;
import tla.backend.es.model.parts.Translations;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "occurrence", type = "occurrence")
public class OccurrenceEntity implements Indexable {

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
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Flexion {

        private String glossing;
        private String verbal;

        @Field(type = FieldType.Keyword)
        private Integer numeric;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Lemmatization {

        private String pos;

        @Field(type = FieldType.Keyword)
        private String id;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
