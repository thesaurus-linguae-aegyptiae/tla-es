package tla.backend.es.model.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.domain.command.PassportSpec;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TextSearch;

public class MappingTest {

    @BeforeEach
    void initModelMapper() {
        ModelConfig.initModelMapper();
    }

    @Test
    void passportSearchCommandMapping() {
        var modelMapper = ModelConfig.modelMapper;
        PassportSpec pp = new PassportSpec();
        pp.put("date", PassportSpec.ThsRefPassportValue.of(List.of("XX"), true));
        var ppp = modelMapper.map(pp, PassportSpec.class);
        assertNotNull(ppp);
        TextSearch command = new TextSearch();
        command.setPassport(pp);
        var ttt = modelMapper.map(command, TextSearchQueryBuilder.class);
        assertNotNull(ttt.getPassport());
        assertEquals(1, ttt.getPassport().size());
    }

    @Test
    void sentenceSearchCommandMapping() {
        SentenceSearch cmd = new SentenceSearch();
        var modelMapper = ModelConfig.modelMapper;
        var qb = modelMapper.map(cmd, SentenceSearchQueryBuilder.class);
        assertNotNull(qb);
    }

}