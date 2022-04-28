package tla.backend.es.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.NonNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.LinkedEntity;
import tla.backend.es.model.parts.EditorInfo;
import tla.domain.dto.CommentDto;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;


@Data
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSComment")
@TLADTO(CommentDto.class)
@Document(indexName = "comment")
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(of = {"id", "body", "revisionState"}, callSuper = true)
public class CommentEntity extends LinkedEntity implements Indexable {

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

}
