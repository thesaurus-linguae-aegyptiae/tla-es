package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tla.domain.model.ExternalReference;
import tla.domain.model.ObjectReference;

@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class TLAEntity extends IndexedEntity {

    @Field(type = FieldType.Keyword)
    private String eclass;

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

}