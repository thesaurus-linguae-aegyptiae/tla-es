package tla.backend.es.model.parts;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import tla.domain.model.meta.Resolvable;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ObjectReference implements Resolvable, Comparable<Resolvable> {


    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword)
    private String id;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword, index = false)
    private String _class;
    
    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword)
    private String eclass;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Keyword)
    private String type;

    @EqualsAndHashCode.Include
    @Field(type = FieldType.Text)
    private String name;
    
    @EqualsAndHashCode.Include
    @Field(type = FieldType.Text)
    private int pos;
    
    @EqualsAndHashCode.Include
    @Field(type = FieldType.Text)
    private int variants;

    /**
     * An optional collection of ranges within the referenced object to which
     * the reference's subject refers to specifically. Only be used by
     * annotations, comments, and some subtexts ("glosses").
     */
    @JsonPropertyOrder(alphabetic = true)
    @Field(type = FieldType.Object, index = false)
    private List<Resolvable.Range> ranges;

    @Override
    public int compareTo(Resolvable arg0) {
    	//System.out.println("ID "+this.id);
    	//System.out.println("Arg0 "+arg0.getId());
    	int resultCompare=-2;
    	//System.out.println("Compare result before"+ resultCompare);
    	if((this.id!=null)&&(arg0.getId()!=null))
          resultCompare= this.id.compareTo(arg0.getId());
    	else if((this.id==null)&&(arg0.getId()==null))
    		resultCompare= 0;
    	else resultCompare= -1;
    	if (this.id==null) this.id="composed";
    	//System.out.println("Compare result "+ resultCompare);
    	return resultCompare;
    }

    /**
     * overrides a reference's token range register with the <b>distinct values</b>
     * in the list passed.
     */
    public void setRanges(List<Resolvable.Range> ranges) {
        if (ranges != null) {
            this.ranges = ranges.stream().distinct().collect(Collectors.toList());
        } else {
            this.ranges = null;
        }
    }

    @EqualsAndHashCode.Include
    private List<Resolvable.Range> sortedUniqueRanges() {
        return (this.ranges != null) ? List.copyOf(Set.copyOf(this.ranges)) : null;
    }

}