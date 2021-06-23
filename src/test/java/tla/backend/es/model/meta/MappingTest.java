package tla.backend.es.model.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.modelmapper.ModelMapper;

import tla.backend.Util;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.query.SentenceSearchQueryBuilder;
import tla.backend.es.query.TextSearchQueryBuilder;
import tla.domain.command.PassportSpec;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TextSearch;
import tla.domain.dto.ThsEntryDto;

@TestInstance(Lifecycle.PER_CLASS)
public class MappingTest {

    private ModelMapper modelMapper;

    public ModelMapper getModelMapper() {
        if (modelMapper == null) {
            modelMapper = ModelConfig.initModelMapper();
        }
        return modelMapper;
    }

    @BeforeAll
    void init() {
        getModelMapper();
    }

    @Test
    void passportSearchCommandMapping() {
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
        var qb = modelMapper.map(cmd, SentenceSearchQueryBuilder.class);
        assertNotNull(qb);
    }

    @Test
    void objectReferenceEquality() throws Exception {
        ThsEntryEntity ths = Util.loadSampleFile(ThsEntryEntity.class, "E7YEQAEKZVEJ5PX7WKOXY2QEEM");
        ThsEntryDto dto = modelMapper.map(ths, ThsEntryDto.class);
        tla.domain.model.ObjectReference dtoRef = tla.domain.model.ObjectReference.from(dto);
        tla.domain.model.ObjectReference refDto = ths.toDTOReference();
        assertEquals(refDto, dtoRef, "DTO-style object reference extracted from entity should equal object reference extracted from DTO");
    }

}