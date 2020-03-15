package tla.backend.es.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Util {

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Returns a JSON serialization of the entity object passed. Name of the
     * corresponding ES index is in the root node.
     *
     * @param entity
     * @return
     */
    public static String toJSON(Indexable entity) {
        try {
            return String.format(
                "{\"%s\": %s}",
                ModelConfig.getIndexName(entity.getClass()),
                objectMapper.writeValueAsString(entity)
            );
        } catch (JsonProcessingException e) {
            log.error(
                String.format(
                    "could not serialize entity instance %s",
                    entity.getId()
                ),
                e
            );
            return entity.getId();
        }
    }
    
}