package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import tla.domain.dto.TextDto;
import tla.domain.model.ExternalReference;
import tla.domain.model.ObjectReference;
import tla.domain.model.Passport;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

/**
 * Text and Subtext model
 */
@Getter
@Setter
@SuperBuilder
@BTSeClass("BTSText")
@TLADTO(TextDto.class)
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "text", type = "text")
public class TextEntity extends TLAEntity {

    @Field(type = FieldType.Keyword)
    String corpus;

    @Field(type = FieldType.Object)
    private Passport passport;

    @Field(type = FieldType.Object)
    List<List<ObjectReference>> paths;

    @Field(type = FieldType.Keyword)
    List<String> sentences;

    @Singular
    @Field(type = FieldType.Object)
    private Map<String, List<ExternalReference>> externalReferences;

    public TextEntity() {
        this.externalReferences = Collections.emptyMap();
    }

}