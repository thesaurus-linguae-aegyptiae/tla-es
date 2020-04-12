package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import tla.domain.dto.DocumentDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.AbstractBTSBaseClass;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseEntity extends AbstractBTSBaseClass implements Indexable {

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
    private String name;

    @Field(type = FieldType.Object)
    private EditorInfo editors;

    @Singular
    @Field(type = FieldType.Object)
    private Map<String, List<ObjectReference>> relations;

    public BaseEntity() {
        this.relations = Collections.emptyMap();
    }

    /**
     * Converts an instance to a DTO of the type specified via {@link TLADTO} annotation
     */
    public DocumentDto toDTO() {
        return ModelConfig.toDTO(this);
    }

        /**
     * Creates an objectreference object identifying this instance.
     */
    public ObjectReference toObjectReference() {
        return ObjectReference.builder()
            .id(this.getId())
            .eclass(this.getEclass())
            .type(this.getType())
            .name(this.getName())
            .build();
    }

}