package tla.backend;

import java.util.Date;
import java.util.Map;

import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.ModelConfig;

public class Util {

    static final Map<Class<? extends Indexable>, String> ENTITY_SAMPLE_PATHS = Map.of(
        SentenceEntity.class, "sentence",
        AnnotationEntity.class, "annotation",
        TextEntity.class, "text"
    );

    public static <E extends Indexable> E loadSampleFile(Class<E> entityClass, String id) throws Exception {
        return tla.domain.util.IO.loadFromFile(
            String.format(
                "src/test/resources/sample/%s/%s.json",
                ENTITY_SAMPLE_PATHS.get(entityClass), id
            ),
            entityClass
        );
    }

    /**
     * create Date instance from ISO-8601 conforming string
     */
    public static Date date(String date) {
        try {
            return ModelConfig.DATE_FORMAT.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

}