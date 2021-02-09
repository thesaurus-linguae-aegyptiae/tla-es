package tla.backend.es.query;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.jayway.jsonpath.Configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.modelmapper.ModelMapper;

import tla.backend.es.model.meta.MappingTest;
import tla.domain.command.LemmaSearch;
import tla.domain.command.SentenceSearch;
import tla.domain.command.TranslationSpec;
import tla.domain.command.TypeSpec;
import tla.domain.model.Language;
import tla.domain.model.Script;

@TestInstance(Lifecycle.PER_CLASS)
public class SearchCommandQueryBuilderTest {

    ModelMapper modelMapper;

    @BeforeAll
    void initModelMapper() {
        modelMapper = new MappingTest().getModelMapper();
    }

    static Object toJson(TLAQueryBuilder query) {
        return Configuration.defaultConfiguration().jsonProvider().parse(
            query.toJson()
        );
    }

    @Test
    void lemmaSearchQueryTest() throws Exception {
        LemmaSearch cmd = new LemmaSearch();
        cmd.setWordClass(new TypeSpec("type", "subtype"));
        cmd.setScript(new Script[]{Script.DEMOTIC});
        var query = modelMapper.map(cmd, LemmaSearchQueryBuilder.class);
        var json = toJson(query);
        assertAll("lemma search ES query",
            //() -> assertEquals("", query.toJson()),
            () -> assertEquals(List.of("type"), read(json, "$.bool.must[*].bool.must[*].term.type.value"), "type term query"),
            () -> assertEquals(List.of("d"), read(json, "$.bool.filter[*].bool.must[*].prefix.id.value"), "prefix for demotic IDs")
        );
    }

    @Test
    @SuppressWarnings("rawtypes")
    void sentenceSeachQueryTest() throws Exception {
        SentenceSearch cmd = new SentenceSearch();
        cmd.setTranslation(new TranslationSpec());
        cmd.getTranslation().setText("horse");
        cmd.getTranslation().setLang(new Language[]{Language.DE, Language.EN});
        SentenceSearch.TokenSpec t = new SentenceSearch.TokenSpec();
        t.setTranslation(new TranslationSpec());
        t.getTranslation().setText("pferd");
        t.getTranslation().setLang(new Language[]{Language.DE});
        cmd.setTokens(List.of(t));
        var query = modelMapper.map(cmd, SentenceSearchQueryBuilder.class);
        var json = toJson(query);
        assertAll("sentence search ES query",
            //() -> assertEquals("", query.toJson()),
            () -> assertEquals(2, ((List)read(json, "$.bool.should[*].bool.should[*].match.keys()")).size()),
            () -> assertEquals(
                List.of("pferd"),
                read(json, "$.bool.filter[*].nested.query.bool.should[*].bool.should[*].match['tokens.translations.de'].query")
            )
        );
    }

}
