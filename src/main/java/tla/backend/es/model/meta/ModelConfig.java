package tla.backend.es.model.meta;

import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.backend.es.model.AnnotationEntity;
import tla.backend.es.model.CorpusObjectEntity;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.SentenceEntity;
import tla.backend.es.model.TextEntity;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.model.parts.EditorInfo;
import tla.backend.es.model.parts.Token;
import tla.backend.es.model.parts.Translations;
import tla.domain.dto.AnnotationDto;
import tla.domain.dto.CorpusObjectDto;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.SentenceDto;
import tla.domain.dto.TextDto;
import tla.domain.dto.ThsEntryDto;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.SentenceToken;
import tla.domain.model.extern.AttestedTimespan;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

/**
 * If a model class is to be added, it has to be annotated with {@link @BTSeClass} and {@link TLADTO}
 * and registered in here.
 */
@Slf4j
@Configuration
public class ModelConfig {

    /**
     * Container for configurations that can be attributed to an eClass specified
     * via {@link BTSeClass} annotation on top of an {@link TLAEntity} instance:
     *
     * <ul>
     * <li>The ES index into which model class instances get saved</li>
     * <li>The model class itself</li>
     * </ul>
     */
    @Getter
    @Builder
    public static class BTSeClassConfig {
        private String index;
        private Class<? extends AbstractBTSBaseClass> modelClass;
    }

    public static SimpleDateFormat DATE_FORMAT;
    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static ModelMapper modelMapper;

    @Getter
    private static List<Class<? extends AbstractBTSBaseClass>> modelClasses = new LinkedList<>();

    @Getter
    private static Map<String, BTSeClassConfig> eclassConfigs = new HashMap<>();

    public ModelConfig() {
        initModelMapper();
    }

    /**
     * Register eClass configuration extracted from the following annotations on the given model class:
     *
     * <ul>
     * <li>{@link BTSeClass}</li>
     * <li>{@link org.springframework.data.elasticsearch.annotations.Document}</li>
     * </ul>
     *
     * @return map with the extracted eclass as its only key,
     *         or <code>null</code> if any config value could not be extracted
     */
    private static Map<String, BTSeClassConfig> mapModelClassConfigToEclass(Class<? extends AbstractBTSBaseClass> clazz) {
        String eclass = null;
        String index = null;
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation instanceof BTSeClass) {
                eclass = ((BTSeClass) annotation).value();
            } else if (annotation instanceof Document) {
                index = ((Document) annotation).indexName();
            }
        }
        BTSeClassConfig config = BTSeClassConfig.builder()
            .index(index)
            .modelClass(clazz)
            .build();
        log.info(
            "register configuration for eClass {}: {}",
            eclass,
            config.toString()
        );
        if (List.of(eclass, index, clazz).stream().allMatch(
            value -> value != null
        )) {
            return Map.of(
                eclass,
                config
            );
        } else {
            log.error(
                "Could not register eclass {} with index {} and model class {}!",
                eclass,
                index,
                clazz
            );
            return null;
        }
    }

    /**
     * extract <code>eclass</code> from {@link BTSeClass} annotation of given model class
     */
    public static String getEclass(Class<? extends BaseEntity> modelClass) {
        for (Annotation annotation : modelClass.getAnnotations()) {
            if (annotation instanceof BTSeClass) {
                return ((BTSeClass) annotation).value();
            }
        }
        log.warn("class {} seems to have no BTSeClass annotation!", modelClass.getName());
        return null;
    }

    /**
     * Register a model class annotated with {@link BTSeClass} and <code>@Document(index="...")</code>
     * and the corresponding configuration extracted from these annotations.
     *
     * <p>Throws an exception if any of the annotations above are missing</p>
     *
     * TODO what does this enable?
     */
    public static Map<String, BTSeClassConfig> registerModelClass(
        Class<? extends AbstractBTSBaseClass> modelClass
    ) throws Exception {
        Map<String, BTSeClassConfig> conf = mapModelClassConfigToEclass(modelClass);
        if (conf != null) {
            if (!modelClasses.contains(modelClass)) {
                modelClasses.add(modelClass);
            }
            eclassConfigs.putAll(conf);
            return conf;
        } else {
            throw new Exception(
                String.format(
                    "could not register model class %s: annotation or annotation parameter missing!",
                    modelClass.getName()
                )
            );
        }
    }

    /**
     * Look up the model class registered for a given eclass.
     */
    public static Class<? extends AbstractBTSBaseClass> getModelClass(String eclass) {
        try {
            return eclassConfigs.get(eclass).getModelClass();
        } catch (Exception e) {
            log.error(
                String.format(
                    "can't find entity model class corresponding to eclass %s. Known eclasses are: %s",
                    eclass,
                    eclassConfigs.keySet()
                )
            );
            throw e;
        }
    }

    /**
     * Tells whether the {@link ModelConfig} class has been instantiated (presumably by spring DI).
     */
    public static boolean isInitialized() {
        return getEclassConfigs() != null && modelMapper != null;
    }

    @Bean
    public ModelMapper modelMapper() {
        if (modelMapper == null) {
            initModelMapper();
        }
        return modelMapper;
    }

    private static ModelMapper initModelMapper() {
        log.debug("initializing model mapper");
        modelMapper = new ModelMapper();
        Translations.ToMapConverter translationsToMapConverter = new Translations.ToMapConverter();
        modelMapper.createTypeMap(EditorInfo.class, tla.domain.model.EditorInfo.class)
            .addMapping(
                EditorInfo::getUpdated, tla.domain.model.EditorInfo::setDateOfLatestUpdate
            ).addMapping(
                EditorInfo::getCreated, tla.domain.model.EditorInfo::setCreationDate
            );
        modelMapper.createTypeMap(LemmaEntity.class, LemmaDto.class)
            .addMappings(
                m -> m.using(translationsToMapConverter).map(
                    LemmaEntity::getTranslations, LemmaDto::setTranslations
                )
            ).addMapping(
                LemmaEntity::getRevisionState, LemmaDto::setReviewState
            );
        modelMapper.createTypeMap(LemmaEntity.AttestedTimeSpan.class, AttestedTimespan.Period.class);
        modelMapper.createTypeMap(ThsEntryEntity.class, ThsEntryDto.class)
            .addMappings(
                m -> m.using(translationsToMapConverter).map(
                    ThsEntryEntity::getTranslations, ThsEntryDto::setTranslations
                )
            ).addMapping(
                ThsEntryEntity::getRevisionState, ThsEntryDto::setReviewState
            );
        modelMapper.createTypeMap(TextEntity.class, TextDto.class)
            .addMapping(
                TextEntity::getRevisionState, TextDto::setReviewState
            );
        modelMapper.createTypeMap(CorpusObjectEntity.class, CorpusObjectDto.class)
            .addMapping(
                CorpusObjectEntity::getRevisionState, CorpusObjectDto::setReviewState
            );
        modelMapper.createTypeMap(AnnotationEntity.class, AnnotationDto.class).addMapping(
            AnnotationEntity::getRevisionState, AnnotationDto::setReviewState
        );
        modelMapper.createTypeMap(SentenceEntity.class, SentenceDto.class).addMappings(
            m -> m.using(translationsToMapConverter).map(
                SentenceEntity::getTranslations, SentenceDto::setTranslations
            )
        );
        modelMapper.createTypeMap(Token.Lemmatization.class, SentenceToken.Lemmatization.class).addMapping(
            Token.Lemmatization::getPartOfSpeech, SentenceToken.Lemmatization::setPartOfSpeech
        );
        modelMapper.createTypeMap(Token.class, SentenceToken.class).addMappings(
            m -> m.using(translationsToMapConverter).map(
                Token::getTranslations, SentenceToken::setTranslations
            )
        );
        return modelMapper;
    }

    /**
     * returns the ES index name specified for an entity class via the <code>indexName</code>
     * attribute of the {@link Document} annotation.
     */
    public static String getIndexName(Class<? extends Indexable> clazz) {
        for (Annotation annotation : clazz.getAnnotationsByType(Document.class)) {
            return ((Document) annotation).indexName();
        }
        throw new IllegalArgumentException(
            String.format(
                "Class %s has no index name specified",
                clazz.getName()
            )
        );
    }

    /**
     * Uses the modelmapper bean configured in {@link #modelMapper()} to convert a model class
     * instance to its corresponding DTO representation, which is specified via a {@link TLADTO}
     * annotation on top of that model class.
     *
     * @param entity An instance of an {@link Indexable} which has a {@link TLADTO} annotation.
     * @return An instance of the DTO class corresponding to the passed entity's class,
     * created using the application context's model mapper instance.
     */
    public static AbstractDto toDTO(Indexable entity) throws NullPointerException {
        if (entity != null) {
            Class<? extends AbstractDto> dtoClass = getModelClassDTO(entity.getClass());
            Object dto = modelMapper.map(
                entity,
                dtoClass
            );
            return dtoClass.cast(dto);
        }
        throw new NullPointerException("can't convert null object!");
    }

    /**
     * Extracts the corresponding DTO type of a model class from its {@link TLADTO} annotation.
     * Returns null and fails silently if no annotation is set.
     *
     * @param modelClass Any {@link Indexable} class which defines its respective DTO class with
     * a {@link TLADO} annotation.
     * @return A {@link AbstractBTSBaseClass} subclass.
     */
    public static Class<? extends AbstractDto> getModelClassDTO(Class<? extends Indexable> modelClass) {
        for (Annotation annotation : modelClass.getAnnotations()) {
            if (annotation instanceof TLADTO) {
                return ((TLADTO) annotation).value();
            }
        }
        log.warn("class {} has not @TLADTO annotation", modelClass.getName());
        return null;
    }

}
