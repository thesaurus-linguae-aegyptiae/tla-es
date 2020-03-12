package tla.backend.es.model;

import java.lang.annotation.Annotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import tla.domain.model.meta.AbstractBTSBaseClass;

@Data
@Slf4j
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class IndexedEntity extends AbstractBTSBaseClass {

    @Id
    @NonNull
    @Field(type = FieldType.Keyword)
    private String id;

    public static String toJSON(IndexedEntity entity) {
        try {
            return String.format(
                "%s: %s",
                ModelConfig.getIndexName(entity.getClass()),
                new ObjectMapper().writeValueAsString(entity)
            );
        } catch (JsonProcessingException e) {
            log.error(
                String.format("could not serialize entity instance %s", entity.id),
                e
            );
            return entity.id;
        }
    }

}
