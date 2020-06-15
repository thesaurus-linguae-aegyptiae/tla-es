package tla.backend.es.model.parts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import tla.domain.model.meta.Resolvable;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObjectReference implements Resolvable, Comparable<Resolvable> {

    @NonNull
    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword)
    private String id;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword)
    private String eclass;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword)
    private String type;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Text)
    private String name;

    /**
     * An optional collection of ranges within the referenced object to which
     * the reference's subject refers to specifically. Only be used by
     * annotations, comments, and some subtexts ("glosses").
     */
    @Singular
    @JsonPropertyOrder(alphabetic = true)
    @Field(type = FieldType.Object, index = false)
    private List<Resolvable.Range> ranges;

    public ObjectReference() {
        this.ranges = Collections.emptyList();
    }

    @Override
    public int compareTo(Resolvable arg0) {
        return this.id.compareTo(arg0.getId());
    }

    public void setRanges(List<Resolvable.Range> ranges) {
        this.ranges = (ranges != null) ? new ArrayList<>(Set.copyOf(ranges)) : Collections.emptyList();
    }

    @EqualsAndHashCode.Include
    private List<Resolvable.Range> sortedUniqueRanges() {
        return (this.ranges != null) ? List.copyOf(Set.copyOf(this.ranges)) : null;
    }

}