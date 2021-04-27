package tla.backend.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.es.repo.RepoConfig;
import tla.backend.es.repo.RepoPopulator;
import tla.backend.service.EntityService;
import tla.backend.service.ModelClass;
import tla.domain.command.SearchCommand;
import tla.domain.dto.extern.SearchResultsWrapper;

@Tag("search")
@SpringBootTest(classes = {SearchTest.TestContext.class})
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public class SearchTest {

    public static final Pageable PAGE_1 = PageRequest.of(0, 20);

    @Configuration
    @Import({RepoConfig.class, ModelConfig.class})
    @ComponentScan(basePackages = {"tla.backend.service"})
    static class TestContext {}

    @Value("classpath:${tla.searchtest.path}/**/*.json")
    private Resource[] testSpecFiles;

    @Autowired
    private RepoPopulator registry;

    @BeforeAll
    void init() {
        registry.init();
    }

    /**
     * Produce all search test specifications found across JSON files within the
     * directory specified by application property <code>tla.searchtest.path</code>.
     */
    private Stream<Arguments> testSpecs() {
        return Arrays.stream(this.testSpecFiles).flatMap(
            specsFile -> this.readTestSpecsFile(specsFile)
        );
    }

    /**
     * read a single search tests specification JSON file under the directory
     * located by the application property <code>tla.searchtest.path</code>.
     * The JSON file must be within a subdirectory whose name is used in a
     * {@link ModelClass} annotation on top of the {@link EntityService} to be
     * used for executing the search commands listed in it.
     */
    private Stream<Arguments> readTestSpecsFile(Resource specsFile) {
        try {
            var filename = specsFile.getFilename().replaceAll(
                "\\.json$", ""
            );
            var path = Paths.get(specsFile.getURI());
            var objectType = path.subpath(
                path.getNameCount() - 2, path.getNameCount() -1
            ).toString();
            return Arrays.stream(
                tla.domain.util.IO.getMapper().readValue(
                    specsFile.getFile(),
                    SearchTestSpecs[].class
                )
            ).map(
                specs -> Arguments.of(Named.of(specs.getName(), specs), objectType, filename)
            );
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    @Test
    void registryReady() throws Exception {
        assertTrue(ModelConfig.isInitialized(), "model class registry is initialized");
        assertTrue(ModelConfig.getModelClasses().contains(LemmaEntity.class), "lemma model registered");
        assertTrue(EntityService.getRegisteredModelClasses().size() > 3, "service registry ready");
    }

    /**
     * Takes a test specification object (consisting of at least a name, a {@link SearchCommand}, and
     * some basic assumptions), and executes it against the entity service responsible for the specified
     * object type.
     */
    @ParameterizedTest(name = "{1} - {2}: {0}")
    @MethodSource("testSpecs")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void searchTest(SearchTestSpecs testSpecs, String objectType, String filename) throws Exception {
        SearchCommand cmd = testSpecs.getCmd();
        EntityService<?,?,?> service = registry.getService(objectType);
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
