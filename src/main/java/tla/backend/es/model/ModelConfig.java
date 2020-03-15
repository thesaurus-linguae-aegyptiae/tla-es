package tla.backend.es.model;

import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;

import tla.domain.dto.LemmaDto;

@Configuration
public class ModelConfig {

    public static SimpleDateFormat DATE_FORMAT;
    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.createTypeMap(LemmaEntity.class, LemmaDto.class)
            .addMappings(
                mapper -> mapper.using(new Translations.ToMapConverter()).map(
                    LemmaEntity::getTranslations, LemmaDto::setTranslations
                )
            )
            .addMappings(
                mapper -> mapper.map(
                    LemmaEntity::getRevisionState, LemmaDto::setReviewState
                )
            );

        return modelMapper;
    }

    /**
     * returns the ES index name specified for an entity class via the <code>indexName</code>
     * attribute of the {@link Document} annotation.
     */
    public static String getIndexName(Class<? extends Indexable> clazz) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation instanceof Document) {
                return ((Document) annotation).indexName();
            }
        }
        throw new IllegalArgumentException(
            String.format(
                "Class %s has no specified index name",
                clazz.getName()
            )
        );
    }
}