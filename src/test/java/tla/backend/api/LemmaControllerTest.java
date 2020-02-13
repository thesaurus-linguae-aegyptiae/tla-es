package tla.backend.api;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import tla.backend.App;
import tla.backend.es.model.Lemma;
import tla.backend.es.repo.LemmaRepo;
import tla.backend.es.model.EditorInfo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(classes = {App.class})
@AutoConfigureMockMvc
public class LemmaControllerTest {

    // https://www.briansdevblog.com/2017/05/rest-endpoint-testing-with-mockmvc/
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LemmaRepo repo;

    @Test
    void postLemma() throws Exception {
        //https://www.petrikainulainen.net/programming/spring-framework/unit-testing-of-spring-mvc-controllers-rest-api/
        mockMvc.perform(
            post("/lemma/post")
                .contentType("application/json")
                .content("{\"id\":\"1\"}")
        )
            .andExpect(
                status().isOk()
            );
    }

    @Test
    void getInexistentLemma() throws Exception {
        mockMvc.perform(
            get("/lemma/get/10070")
                .contentType("application/json")
        )
            .andExpect(
                status().isNotFound()
            );
    }

    @Test
    void getExistingLemma() throws Exception {
        when(repo.findById(anyString()))
            .thenReturn(
                Optional.of(
                    Lemma.builder()
                        .id("ID")
                        .eclass("BTSLemmaEntry")
                        .editors(
                            EditorInfo.builder()
                                .author("author")
                                .build()
                        )
                        .build()
                )
            );
        mockMvc.perform(
            get("/lemma/get/whatever")
                .contentType("application/json")
        )
            .andExpect(
                status().isOk()
            )
            .andExpect(jsonPath("$.id").value("ID"))
            .andExpect(jsonPath("$.editors.author").value("author"));
    }

}