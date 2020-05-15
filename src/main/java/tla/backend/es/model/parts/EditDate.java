package tla.backend.es.model.parts;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import org.springframework.data.elasticsearch.core.convert.ElasticsearchDateConverter;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class EditDate implements TemporalAccessor, Serializable, Comparable<ChronoLocalDate> {

    public static ElasticsearchDateConverter DATE_CONVERTER = ElasticsearchDateConverter.of(
        "yyyy-MM-dd"
    );

    private static final long serialVersionUID = -2478025723214725811L;

    private LocalDate date;

    public EditDate(LocalDate date) {
        this.date = date;
    }

    @JsonCreator
    public static EditDate fromString(String source) throws Exception {
        String[] coords = source.split("-");
        if (coords.length == 3) {
            int year = Integer.valueOf(coords[0]);
            int mon = Integer.valueOf(coords[1]);
            int day = coords[2].length() < 3 ? Integer.valueOf(coords[2]) : Integer.valueOf(coords[2].split(" ")[0]);
            return new EditDate(LocalDate.of(year, mon, day));
        } else {
            throw new Exception(String.format("could not create date instance from input '%s'", source));
        }
    }

    @JsonValue
    @EqualsAndHashCode.Include
    public String toString() {
        return DATE_CONVERTER.format(date);
    }

    public static EditDate of(int year, int month, int day) {
        return new EditDate(
            LocalDate.of(year, month, day)
        );
    }

    public static EditDate from(TemporalAccessor temporal) {
        return new EditDate(
            LocalDate.of(
                temporal.get(ChronoField.YEAR_OF_ERA),
                temporal.get(ChronoField.MONTH_OF_YEAR),
                temporal.get(ChronoField.DAY_OF_MONTH)
            )
        );
    }

    @Override
    public long getLong(TemporalField arg0) {
        if (this.date.isSupported(arg0)) {
            return this.date.getLong(arg0);
        } else {
            return 0;
        }
    }

    @Override
    public boolean isSupported(TemporalField arg0) {
        return date.isSupported(arg0);
    }

    @Override
    public int compareTo(ChronoLocalDate arg0) {
        return date.compareTo(arg0);
    }

}