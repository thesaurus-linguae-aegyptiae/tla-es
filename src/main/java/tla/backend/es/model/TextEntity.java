package tla.backend.es.model;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tla.domain.model.ObjectReference;
import tla.domain.model.Passport;
import tla.domain.model.meta.BTSeClass;

/**
 * Text and Subtext model
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@BTSeClass("BTSText")
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

}