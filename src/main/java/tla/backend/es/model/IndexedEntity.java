package tla.backend.es.model;

import java.lang.annotation.Annotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import tla.domain.model.meta.BTSeClass;

@Data
@Slf4j
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class IndexedEntity {

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

    /**
     * Returns the object's <code>eClass</code> value specified via the {@link BTSeClass} annotation.
     */
    public String getEclass() {
        for (Annotation annotation : this.getClass().getAnnotations()) {
            if (annotation instanceof BTSeClass) {
                return ((BTSeClass) annotation).value();
            }
        }
        log.warn(
            "eClass of {} instance not specified via @BTSeClass annotation. Returning class name",
            this.getClass().getName()
        );
        return this.getClass().getName();
    }

}