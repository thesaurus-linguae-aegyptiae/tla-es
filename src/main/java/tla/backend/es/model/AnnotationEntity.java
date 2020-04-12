package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import tla.domain.dto.AnnotationDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Data
@SuperBuilder
@AllArgsConstructor
@BTSeClass("BTSAnnotation")
@TLADTO(AnnotationDto.class)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Document(indexName = "annotation", type = "annotation")
public class AnnotationEntity extends AbstractBTSBaseClass implements Indexable {

    @Id
    @NonNull
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Keyword)
    private String subtype;

    @Field(type = FieldType.Keyword)
    private String revisionState;

    @Field(type = FieldType.Text)
    @JsonAlias({"name"})
    private String title;

    @Field(type = FieldType.Object)
    private EditorInfo editors;

    @Singular
    @Field(type = FieldType.Object)
    private Map<String, List<ObjectReference>> relations;

    public AnnotationEntity() {
        this.relations = Collections.emptyMap();
    }
}