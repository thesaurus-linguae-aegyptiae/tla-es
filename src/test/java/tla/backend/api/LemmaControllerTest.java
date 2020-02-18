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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(classes = {App.class})
@AutoConfigureMockMvc
public class LemmaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LemmaRepo repo;

    @Test
    void nonNullFieldValidation() {
        assertThrows(NullPointerException.class,
            () -> {Lemma.builder().build();},
            "building lemma with null-ID should throw exception"
        );
    }

    @Test
    void postLemma() throws Exception {
        when(repo.save(any()))
            .thenReturn(Lemma.builder().id("1").sortKey("A").build());
        Lemma l = repo.save(Lemma.builder().id("2").build());
        assertEquals("1", l.getId(), "whatever is being saved by mock up repo, it should always return a lemma with ID '1'");
        mockMvc.perform(
            post("/lemma/post")
                .contentType("application/json")
                .content("{\"id\":\"1\",\"sort_string\":\"A\"}")
        )
            .andExpect(
                status().isOk()
            )
            .andExpect(jsonPath("$.id").value("1"))
            .andExpect(jsonPath("$.sortKey").value("A"));
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