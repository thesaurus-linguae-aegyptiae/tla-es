package tla.backend.es.model.parts;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import tla.domain.model.ObjectReference.Range;
import tla.domain.model.meta.Resolvable;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectReference implements Resolvable, Comparable<Resolvable> {

    @NonNull
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Keyword)
    private String eclass;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String type;

    public ObjectReference(){}

    /**
     * An optional collection of ranges within the referenced object to which
     * the reference's subject refers to specifically. Only be used by
     * annotations, comments, and some subtexts ("glosses").
     */
    @Field(type = FieldType.Object, index = false)
    private List<Range> ranges;

    @Override
    public int compareTo(Resolvable arg0) {
        return this.getId().compareTo(arg0.getId());
    }

}