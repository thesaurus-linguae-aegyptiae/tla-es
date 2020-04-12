package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tla.domain.dto.LemmaDto;
import tla.domain.model.ExternalReference;
import tla.domain.model.Passport;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Data
@SuperBuilder
@AllArgsConstructor
@TLADTO(LemmaDto.class)
@BTSeClass("BTSLemmaEntry")
@ToString(callSuper = true)
@JsonInclude(Include.NON_EMPTY)
@EqualsAndHashCode(callSuper = true)
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

    @Singular
    @Field(type = FieldType.Object)
    private List<LemmaWord> words;

    @Singular
    @Field(type = FieldType.Object)
    private Map<String, List<ExternalReference>> externalReferences;

    public LemmaEntity() {
        this.words = Collections.emptyList();
        this.externalReferences = Collections.emptyMap();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(Include.NON_NULL)
    private static class AttestedTimeSpan {
        @Field(type = FieldType.Integer)
        private Integer begin;

        @Field(type = FieldType.Integer)
        private Integer end;
    }
}
