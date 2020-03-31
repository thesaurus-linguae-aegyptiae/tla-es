package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tla.domain.dto.DocumentDto;
import tla.domain.model.ExternalReference;
import tla.domain.model.ObjectReference;

import tla.domain.model.meta.AbstractBTSBaseClass;

/**
 * TLA model base class for BTS document types.
 */
@Getter
@Setter
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class TLAEntity extends AbstractBTSBaseClass implements Indexable {

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

    @Singular
    @Field(type = FieldType.Object)
    private Map<String, List<ExternalReference>> externalReferences;

    public TLAEntity() {
        this.relations = Collections.emptyMap();
        this.externalReferences = Collections.emptyMap();
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

    /**
     * Converts an instance to a DTO of the type specified via {@link TLADTO} annotation
     */
    public DocumentDto toDTO() {
        return ModelConfig.toDTO(this);
    }

}