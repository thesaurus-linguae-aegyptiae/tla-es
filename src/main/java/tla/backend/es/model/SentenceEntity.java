package tla.backend.es.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.parts.Transcription;
import tla.backend.es.model.parts.Translations;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.backend.es.model.parts.Token;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@BTSeClass("BTSSentence")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "sentence", type = "sentence")
@Setting(settingPath = "/elasticsearch/settings/indices/sentence.json")
public class SentenceEntity extends AbstractBTSBaseClass implements Indexable {

    @Id
    @NonNull
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    @JsonAlias("text")
    private String textId;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Text)
    private String line;

    @Field(type = FieldType.Text)
    private String paragraph;

    @Field(type = FieldType.Object)
    private Translations translations;

    @Field(type = FieldType.Object)
    private Transcription transcription;

    @Field(type = FieldType.Nested)
    private Collection<Token> tokens;

}