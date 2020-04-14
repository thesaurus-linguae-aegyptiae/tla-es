package tla.backend.es.model;

import java.util.List;

import org.modelmapper.AbstractConverter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tla.domain.dto.TextDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.Paths;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

/**
 * Text and Subtext model
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSText")
@TLADTO(TextDto.class)
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "text", type = "text")
public class TextEntity extends TLAEntity {

    @Field(type = FieldType.Keyword)
    String corpus;

    @Field(type = FieldType.Object)
    List<List<ObjectReference>> paths;

    @Field(type = FieldType.Keyword)
    List<String> sentences;

    public static class ListToPathsConverter extends AbstractConverter<List<List<ObjectReference>>, Paths> {
        @Override
        protected Paths convert(List<List<ObjectReference>> source) {
            return Paths.of(source);
        }
    }

}