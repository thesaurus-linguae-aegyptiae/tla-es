package tla.backend.es.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import tla.backend.es.repo.RepoPopulator;
import tla.domain.model.ExternalReference;
import tla.domain.model.Passport;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

/**
 * TLA model base class for BTS document types.
 * Implementing subclasses should be annotated with {@link BTSeClass}, {@link TLADTO},
 * and registered in both {@link ModelConfig} and {@link RepoPopulator}.
 */
@Getter
@Setter
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class TLAEntity extends BaseEntity {

    @Singular
    @Field(type = FieldType.Object)
    private Map<String, List<ExternalReference>> externalReferences;

    @Field(type = FieldType.Object)
    private Passport passport;

    /**
     * Default constructor initializing the externalReferences map as an empty object.
     */
    public TLAEntity() {
        this.externalReferences = Collections.emptyMap();
    }

}
