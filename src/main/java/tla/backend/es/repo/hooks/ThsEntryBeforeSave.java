package tla.backend.es.repo.hooks;

import org.springframework.data.elasticsearch.core.event.BeforeConvertCallback;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import tla.backend.es.model.ThsEntryEntity;

/**
 * Callback for intercepting {@link ThsEntryEntity} objects just before they get
 * converted into Elasticsearch <code>Document</code> instances.
 *
 * This callback's job is to copy thesaurus entry name aliases from its passport
 * into a dedicated field with appropriate search mapping.
 */
@Component
public class ThsEntryBeforeSave implements BeforeConvertCallback<ThsEntryEntity> {

    @Override
    public ThsEntryEntity onBeforeConvert(ThsEntryEntity entity, IndexCoordinates index) {
        entity.setTranslations(
            entity.getTranslations()
        );
        return entity;
    }
    
}