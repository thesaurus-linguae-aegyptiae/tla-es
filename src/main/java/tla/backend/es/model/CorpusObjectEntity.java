package tla.backend.es.model;

import java.util.List;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tla.backend.es.model.meta.TLAEntity;
import tla.domain.dto.CorpusObjectDto;
import tla.domain.model.ObjectReference;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;
import tla.domain.model.meta.UserFriendly;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@BTSeClass("BTSTCObject")
@TLADTO(CorpusObjectDto.class)
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "object", type = "object")
public class CorpusObjectEntity extends TLAEntity implements UserFriendly {

    @Field(type = FieldType.Keyword)
    String corpus;

    @Field(type = FieldType.Object)
    List<List<ObjectReference>> paths;

    @Field(type = FieldType.Keyword)
    private String sUID;

}