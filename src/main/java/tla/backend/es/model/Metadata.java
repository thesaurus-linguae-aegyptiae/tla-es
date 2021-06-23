package tla.backend.es.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.parts.EditDate;
import tla.domain.model.meta.AbstractBTSBaseClass;
import tla.domain.model.meta.BTSeClass;

@Getter
@Setter
@NoArgsConstructor
@BTSeClass("TLAMetadata")
@Document(indexName = "meta")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata extends AbstractBTSBaseClass implements Indexable {

    @Id
    @Field(type = FieldType.Keyword)
    @JsonAlias({"version"})
    private String id;

    @Field(type = FieldType.Keyword)
    @JsonAlias({"DOI"})
    private String DOI;

    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "UTC")
    private EditDate date;

    @Field(type = FieldType.Keyword)
    @JsonAlias({"etl-version"})
    private String etlVersion;

    @Field(type = FieldType.Keyword)
    @JsonAlias({"model-version"})
    private String modelVersion;

    @Field(type = FieldType.Keyword)
    @JsonAlias({"linggloss-version"})
    private String lingglossVersion;

}
