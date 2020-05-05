package tla.backend.es.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.meta.BaseEntity;
import tla.domain.dto.AnnotationDto;
import tla.domain.model.Passport;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Data
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSAnnotation")
@TLADTO(AnnotationDto.class)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Document(indexName = "annotation", type = "annotation")
public class AnnotationEntity extends BaseEntity {

    @Field(type = FieldType.Object)
    private Passport passport;

    @Field(type = FieldType.Keyword)
    private String corpus;

    public void setTitle(String title) {
        this.setName(title);
    }

}