package tla.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import tla.backend.es.model.ThsEntryEntity;

public class ThsServiceTest {


    private ThsEntryEntity loadThsEntryFromFile(String id) throws Exception {
        return tla.domain.util.IO.loadFromFile(
            String.format(
                "src/test/resources/sample/ths/%s.json",
                id
            ),
            ThsEntryEntity.class
        );
    }


    @Test
    void deserializeFromFile() throws Exception {
        ThsEntryEntity t = loadThsEntryFromFile("E7YEQAEKZVEJ5PX7WKOXY2QEEM");
        assertAll("test thesaurus entry deserialized from file",
            () -> assertTrue(t != null, "deserialized entity should not be null"),
            () -> assertTrue(t.getPassport() != null, "passport should not be null"),
            () -> assertEquals(
                1,
                t.getPassport().extractProperty("thesaurus_date.main_group.beginning").size(),
                "begin of time period should exist exactly once"
            )
        );
    }

    @Test
    void singleEntryTimespan() throws Exception {
        ThsEntryEntity t = loadThsEntryFromFile("E7YEQAEKZVEJ5PX7WKOXY2QEEM");
        List<Integer> years = t.extractTimespan();
        assertAll("check if ths entry timespan gets extracted correctly",
            () -> assertEquals(2, years.size(), "should return 2 values"),
            () -> assertEquals(-1076, years.get(0), "earliest year"),
            () -> assertEquals(-944, years.get(1), "latest year")
        );
    }
}