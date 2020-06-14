package tla.backend.es.model.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.parts.EditorInfo;
import tla.backend.es.model.parts.ObjectReference;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.Resolvable;
import tla.domain.model.meta.TLADTO;

/**
 * Entity model base class. Represents an identifiable stand-alone BTS/TLA document with
 * a name, a type and subtype, editing information, and references to related documents.
 *
 * <blockquote>Document types which have a <code>passport</code> metadata tree, and which are supposed
 * to be identifiable as external resources (e.g. in the online database published by a dfferent
 * research project) can extend the specialized abstract subclass {@link TLAEntity}.</blockquote>
 *
 * <p>Because this class implements {@link Indexable}, subclasses are expected
 * to declare an Elasticsearch index they are to be stored in, and because all subclasses are
 * {@link AbstractBTSBaseClass} instances, they must specify the <code>eClass</code> of the
 * original Berlin Text System (BTS) model class they correspond with.
 * Both properties are being registered in {@link ModelConfig}.</p>
 */
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

    /**
     * Information about what researcher authored this document, who contributed to it,
     * and the date of the latest change.
     */
    @Field(type = FieldType.Object)
    private EditorInfo editors;

    /**
     * References to related objects grouped by relationship name (<code>partOf</code>,
     * <code>predecessor</code>, ...).
     */
    @Singular
    @Field(type = FieldType.Object)
    private Map<String, Relations> relations;

    /**
     * Default constructor initializing the relations map as an empty object.
     */
    public BaseEntity() {
        this.relations = Collections.emptyMap();
    }

    /**
     * Converts an instance to a DTO of the type specified via {@link TLADTO} annotation
     */
    public AbstractDto toDTO() {
        return ModelConfig.toDTO(this);
    }

    /**
     * Creates an {@link tla.domain.model.ObjectReference} (DTO model) object identifying this instance.
     */
    public tla.domain.model.ObjectReference toDTOReference() {
        return tla.domain.model.ObjectReference.builder()
            .id(this.getId())
            .eclass(this.getEclass())
            .type(this.getType())
            .name(this.getName())
            .build();
    }

    /**
     * A collection of references to other entity objects.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonDeserialize(contentAs = ObjectReference.class)
    public static class Relations extends ArrayList<Resolvable> {

        private static final long serialVersionUID = -3638905986166571667L;

        public Relations(Collection<Resolvable> refs) {
            this.addAll(refs);
        }

        public static Relations of(Resolvable... sources) {
            return new Relations(
                Arrays.asList(sources)
            );
        }
    }

}