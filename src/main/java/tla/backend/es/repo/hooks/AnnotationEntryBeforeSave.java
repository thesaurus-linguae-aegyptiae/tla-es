package tla.backend.es.repo.hooks;

import org.springframework.data.elasticsearch.core.event.BeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import tla.backend.es.model.AnnotationEntity;

/**
 * Callback intercepting {@link AnnotationEntity} instances before they get converted
 * into Elasticsearch documents. Its purpose is to copy lemma annotation text bodies from
 * passports into a properly mapped field.
 */
@Component
public class AnnotationEntryBeforeSave implements BeforeConvertCallback<AnnotationEntity> {

    @Override
    public AnnotationEntity onBeforeConvert(AnnotationEntity entity, IndexCoordinates index) {
        entity.setBody(
            entity.getBody()
        );
        return entity;
    }

}