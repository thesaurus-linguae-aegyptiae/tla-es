package tla.backend.es.model;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.lang.annotation.Annotation;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.annotations.Document;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tla.domain.dto.LemmaDto;
import tla.domain.model.meta.BTSeClass;

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
    protected static class BTSeClassConfig {
        private String index;
        private Class<? extends TLAEntity> modelClass;
    }

    public static SimpleDateFormat DATE_FORMAT;
    static {
        DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Getter
    private static List<Class<? extends TLAEntity>> modelClasses = new LinkedList<>();

    @Getter
    private static Map<String, BTSeClassConfig> modelClassConfigs;

    public ModelConfig() {
        setModelClasses(
            List.of(
                LemmaEntity.class,
                ThsEntryEntity.class
            )
        );
    }

    /**
     * Extract eclass configurations from all registered model classes.
     */
    private static void init() {
        modelClassConfigs = new HashMap<>();
        modelClasses.forEach(
            clazz -> {
                try {
                    registerModelClass(clazz);
                } catch (Exception e) {
                    log.error(
                        String.format(
                            "config initialization for model class %s failed.",
                            clazz.getName()
                        ),
                        e
                    );
                }
            }
        );
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
    private static Map<String, BTSeClassConfig> mapModelClassConfigToEclass(Class<? extends TLAEntity> clazz) {
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
     * Register a model class annotated with {@link BTSeClass} and <code>@Document(index="...")</code>
     * and the corresponding configuration extracted from these annotations.
     *
     * <p>Throws an exception if any of the annotations above are missing</p>
     *
     * TODO what does this enable?
     */
    public static Map<String, BTSeClassConfig> registerModelClass(
        Class<? extends TLAEntity> modelClass
    ) throws Exception {
        Map<String, BTSeClassConfig> conf = mapModelClassConfigToEclass(modelClass);
        if (conf != null) {
            if (!modelClasses.contains(modelClass)) {
                modelClasses.add(modelClass);
            }
            modelClassConfigs.putAll(conf);
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
     * Clear eclass/model class configuration registry and load configurations for
     * classes passed.
     */
    public static void setModelClasses(List<Class<? extends TLAEntity>> classes) {
        if (modelClassConfigs != null) {
            modelClassConfigs.clear();
            log.info(
                "erase model class registry containing eclasses: {}",
                String.join(", ", modelClassConfigs.keySet())
            );
        } else {
            log.info(
                "initial computation of known model class configurations"
            );
        }
        modelClasses.clear();
        modelClasses.addAll(classes);
        init();
        log.info(
            "configured eclass class registry updated: {}",
            String.join(", ", modelClassConfigs.keySet())
        );
    }

    /**
     * Look up the model class registered for a given eclass.
     */
    public static Class<? extends TLAEntity> getModelClass(String eclass) {
        return modelClassConfigs.get(eclass).getModelClass();
    }

    /**
     * Tells whether the {@link ModelConfig} class has been instantiated (presumably by spring DI).
     */
    public static boolean isInitialized() {
        return getModelClassConfigs() != null;
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
                "Class %s has no index name specified",
                clazz.getName()
            )
        );
    }

}
