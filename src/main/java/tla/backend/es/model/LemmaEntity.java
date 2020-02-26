package tla.backend.es.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tla.domain.model.Passport;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(callSuper = false)
@Document(indexName = "lemma", type = "lemma")
@Setting(settingPath = "/elasticsearch/settings/indices/lemma.json")
public class LemmaEntity extends TLAEntity {

    @Field(type = FieldType.Keyword)
    @JsonAlias({"sortString", "sort_string", "sort_key"})
    private String sortKey;

    @Field(type = FieldType.Object)
    private Passport passport;

    @Field(type = FieldType.Object)
    private Translations translations;

    @Field(type = FieldType.Object)
    @JsonAlias({"time_span"})
    private AttestedTimeSpan timeSpan;

    @Data
    @AllArgsConstructor
    private class AttestedTimeSpan {
        @Field(type = FieldType.Integer)
        private int begin;

        @Field(type = FieldType.Integer)
        private int end;
    }
}