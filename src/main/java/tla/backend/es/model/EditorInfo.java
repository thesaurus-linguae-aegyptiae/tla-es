package tla.backend.es.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class EditorInfo {

    @Field(type = FieldType.Text)
    private String author;

    // FIXME
    // https://github.com/spring-projects/spring-data-elasticsearch/blob/master/src/main/java/org/springframework/data/elasticsearch/core/convert/MappingElasticsearchConverter.java
    @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updated;

    @Field(type = FieldType.Keyword)
    private String type;

    @Nullable
    @Field(type = FieldType.Text)
    private List<String> contributors;

}