package tla.backend.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import tla.backend.App;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.meta.Indexable;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.service.EntityService;
import tla.domain.command.SearchCommand;
import tla.domain.dto.extern.SearchResultsWrapper;
import tla.domain.model.meta.AbstractBTSBaseClass;

@Tag("search")
@SpringBootTest(classes = {App.class})
public class SearchTest {

    public static final Pageable PAGE_1 = PageRequest.of(0, 20);

    private static Stream<Arguments> testSpecs() throws Exception {
        SearchTestSpecs[] scenarios = tla.domain.util.IO.loadFromFile(
            "src/test/resources/search/tests.json",
            SearchTestSpecs[].class
        );
        return Arrays.stream(scenarios).map(
            specs -> Arguments.of(Named.of(specs.getName(), specs))
        );
    }

    public EntityService<?,?,?> getService(Class<? extends AbstractBTSBaseClass> dtoClass) {
        for (Class<? extends Indexable> modelClass : EntityService.getRegisteredModelClasses()) {
            if (ModelConfig.getModelClassDTO(modelClass).equals(dtoClass)) {
                if (AbstractBTSBaseClass.class.isAssignableFrom(modelClass)) {
                    return EntityService.getService(
                        modelClass.asSubclass(AbstractBTSBaseClass.class)
                    );
                }
            }
        }
        return null;
    }

    @Test
    void registryReady() {
        assertTrue(ModelConfig.isInitialized(), "model class registry is initialized");
        assertTrue(ModelConfig.getModelClasses().contains(LemmaEntity.class), "lemma model registered");
        assertTrue(EntityService.getRegisteredModelClasses().size() > 3, "service registry ready");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("testSpecs")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void searchTest(SearchTestSpecs testSpecs) throws Exception {
        SearchCommand cmd = testSpecs.getCmd();
        EntityService<?,?,?> service = getService(cmd.getDTOClass());
        assertNotNull(cmd);
        SearchResultsWrapper<?> result = (SearchResultsWrapper) service.runSearchCommand(cmd, PAGE_1).get();
        testSpecs.getValid().forEach(
            valid -> {
                assertTrue(
                    result.getResults().stream().anyMatch(
                        dto -> dto.getId().equals(valid)
                    ),
                    "expect ID to be found: " + valid
                );
            }
        );
        testSpecs.getInvalid().forEach(
            invalid -> {
                assertFalse(
                    result.getResults().stream().anyMatch(
                        dto -> dto.getId().equals(invalid)
                    ),
                    "expect ID not to be found: " + invalid
                );
            }
        );
    }

}
