package tla.backend.es.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.meta.TLAEntity;
import tla.backend.es.model.parts.ObjectPath;
import tla.backend.es.model.parts.Translations;
import tla.domain.dto.TextDto;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;
import tla.domain.model.meta.UserFriendly;

/**
 * Text and Subtext model
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSText")
@TLADTO(TextDto.class)
@Document(indexName = "text")
public class TextEntity extends TLAEntity implements UserFriendly {

    @Field(type = FieldType.Keyword, name = "hash")
    private String SUID;

    @Field(type = FieldType.Keyword)
    private String corpus;

    @Field(type = FieldType.Object)
    private ObjectPath[] paths;

    @Field(type = FieldType.Object)
    private List<Translations> translations;

    @Field(type = FieldType.Integer)
    @JsonAlias("word_count")
    private int wordCount;

}
