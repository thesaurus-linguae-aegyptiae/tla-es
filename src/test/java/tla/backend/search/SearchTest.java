package tla.backend.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
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
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import tla.backend.App;
import tla.backend.es.model.LemmaEntity;
import tla.backend.es.model.meta.ModelConfig;
import tla.backend.service.EntityService;
import tla.backend.service.LemmaService;
import tla.domain.command.SearchCommand;
import tla.domain.dto.LemmaDto;
import tla.domain.dto.extern.SearchResultsWrapper;
import tla.domain.dto.meta.AbstractDto;
import tla.domain.model.meta.AbstractBTSBaseClass;

@Tag("search")
@SpringBootTest(classes = {App.class})
@ActiveProfiles("test")
@TestInstance(Lifecycle.PER_CLASS)
public class SearchTest {

    @Value("classpath:${tla.searchtest.path}/**/*.json")
    private Resource[] testSpecFiles;

    @Autowired
    private LemmaService lemmaService;

    public static final Pageable PAGE_1 = PageRequest.of(0, 20);
    private Map<Class<? extends AbstractDto>, EntityService<?,?,?>> services;

    @BeforeAll
    void init() {
        this.services = Map.of(
            LemmaDto.class, lemmaService
        );
    }

    private Stream<Arguments> testSpecs() throws Exception {
        return Arrays.stream(this.testSpecFiles).flatMap(
            specsFile -> this.readTestSpecsFile(specsFile)
        );
    }

    public EntityService<?,?,?> getService(Class<? extends AbstractBTSBaseClass> dtoClass) {
        return this.services.getOrDefault(dtoClass, lemmaService);
    }

    private Stream<Arguments> readTestSpecsFile(Resource specsFile) {
        try {
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
                specs -> Arguments.of(Named.of(specs.getName(), specs), objectType)
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

    @ParameterizedTest(name = "{1} - {0}")
    @MethodSource("testSpecs")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void searchTest(SearchTestSpecs testSpecs, String objectType) throws Exception {
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
