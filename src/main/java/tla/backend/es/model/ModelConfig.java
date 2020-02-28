package tla.backend.es.model;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}