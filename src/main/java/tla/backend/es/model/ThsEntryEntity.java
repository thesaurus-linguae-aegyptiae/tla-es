package tla.backend.es.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tla.domain.dto.ThsEntryDto;
import tla.domain.model.Passport;
import tla.domain.model.extern.AttestedTimespan;
import tla.domain.model.meta.BTSeClass;
import tla.domain.model.meta.TLADTO;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@BTSeClass("BTSThsEntry")
@TLADTO(ThsEntryDto.class)
@EqualsAndHashCode(callSuper = true)
@Document(indexName = "ths", type = "ths")
public class ThsEntryEntity extends TLAEntity {

    private static final List<String> timespanPropertyPaths = List.of(
        "thesaurus_date.main_group.beginning",
        "thesaurus_date.main_group.end"
    );

    @Field(type = FieldType.Keyword)
    @JsonAlias({"sortkey", "sort_key", "sort_string", "sortString"})
    private String sortKey;

    @Field(type = FieldType.Object)
    private Passport passport;

    /**
     * Returns the timespan represented by a thesaurus entry.
     */
    public List<Integer> extractTimespan() {
        List<Integer> years = new ArrayList<>();
        timespanPropertyPaths.stream().forEach(
            path -> {
                this.getPassport().extractProperty(path).stream().forEach(
                    node -> {
                        if (node.get() instanceof String) {
                            years.add(Integer.valueOf((String) node.get()));
                        }
                    }
                );
            }
        );
        Collections.sort(years);
        return years;
    }

    public AttestedTimespan.Period toAttestedPeriod() {
        List<Integer> years = this.extractTimespan();
        return AttestedTimespan.Period.builder()
            .begin(years.get(0))
            .end(years.get(1))
            .ths(this.toObjectReference())
            .build();
    }
}