package tla.backend.es.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tla.domain.model.Passport;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "ths", type = "ths")
public class ThsEntryEntity extends TLAEntity {

    @Field(type = FieldType.Keyword)
    @JsonAlias({"sortkey", "sort_key", "sort_string", "sortString"})
    private String sortKey;

    @Field(type = FieldType.Object)
    private Passport passport;

}