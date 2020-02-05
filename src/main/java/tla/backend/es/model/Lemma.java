package tla.backend.es.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.lang.NonNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tla.backend.es.model.EditorInfo;
import tla.domain.model.Passport;

@Data
@Builder
@NoArgsConstructor()
@AllArgsConstructor
@Document(indexName = "lemma", type = "lemma")
public class Lemma {

    @Id
    @NonNull
    private String id;

    private String eclass;

    private String type;

    private String subtype;

    private String revisionState;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Object)
    private Passport passport;

    @Field(type = FieldType.Object)
    private EditorInfo editors;

    @Field(type = FieldType.Object)
    private Translations translations;

}