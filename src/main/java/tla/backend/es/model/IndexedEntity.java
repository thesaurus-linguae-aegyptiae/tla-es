package tla.backend.es.model;

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

}