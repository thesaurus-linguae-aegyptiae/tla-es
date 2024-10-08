package tla.backend.es.model;

import java.util.Collections;
import java.util.List;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.model.parts.Token;
import tla.backend.es.model.parts.Translations;
import tla.backend.es.model.parts.Transcription;
import tla.backend.es.model.parts.Glyphs;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.LemmaDto.TimeSpan;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Data
@TLADTO(LemmaDto.class)
@BTSeClass("BTSLemmaEntry")
@ToString(callSuper = true)
@JsonInclude(Include.NON_NULL)
@Document(indexName = "lemma")
@EqualsAndHashCode(callSuper = true)
@Setting(settingPath = "/elasticsearch/settings/indices/lemma.json")
public class LemmaEntity extends TLAEntity {

    @Field(type = FieldType.Search_As_You_Type, analyzer = "transcription_analyzer")
    private String name;

    @Field(type = FieldType.Keyword)
    @JsonAlias({"sortString", "sort_string", "sort_key"})
    private String sortKey;
    
    @Field(type = FieldType.Object)
    private Glyphs glyphs;
  
    @Field(type = FieldType.Object)
    private Transcription transcription;
    
    @Field(type = FieldType.Object)
    private Translations translations;

    @Field(type = FieldType.Object)
    @JsonAlias({"time_span"})
    private TimeSpan timeSpan;
    
    @Field(type = FieldType.Integer)
    private int attestedSentencesCount;

    @Singular
    @Field(type = FieldType.Object)
    private List<Token> words;

    public LemmaEntity() {
        this.words = Collections.emptyList();
    }
}
