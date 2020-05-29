package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.NonNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.parts.EditorInfo;
import tla.domain.dto.CommentDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;


@Data
@SuperBuilder
@BTSeClass("BTSComment")
@TLADTO(CommentDto.class)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(indexName = "comment", type = "comment")
@ToString(of = {"id", "body", "revisionState"}, callSuper = true)
public class CommentEntity extends AbstractBTSBaseClass implements Indexable {

    @Id
    @NonNull
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Text, analyzer = "german")
    private String body;

    @Field(type = FieldType.Object)
    private EditorInfo editors;

    @Field(type = FieldType.Keyword)
    private String revisionState;

    /**
     * References to related objects grouped by relationship predicate (most likely
     * <code>partOf</code> relations to text objects), as well as possibly one or more
     * representations of text ranges covered within the referenced text objects,
     * consisting of the first and last token within any given range.
     */
    @Singular
    @Field(type = FieldType.Object)
    private Map<String, List<ObjectReference>> relations;

    public CommentEntity() {
        this.relations = Collections.emptyMap();
    }

}