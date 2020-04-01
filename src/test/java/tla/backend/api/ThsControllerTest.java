package tla.backend.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import tla.backend.AbstractMockMvcTest;
import tla.backend.es.model.ThsEntryEntity;
import tla.backend.es.repo.ThesaurusRepo;
import tla.backend.service.ThesaurusService;
import tla.domain.model.ObjectReference;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class ThsControllerTest extends AbstractMockMvcTest {

    @Autowired
    private ThesaurusService service;

    @MockBean
    private ThesaurusRepo repo;

    private ThsEntryEntity thsEntry;

    @BeforeEach
    void init() {
        this.thsEntry = ThsEntryEntity.builder()
        .id("1")
        .eclass("BTSThsEntry")
        .name("wadi")
        .type("findSpot")
        .relation("partOf", Arrays.asList(
            ObjectReference.builder().id("2").name("region1").type("findSpot").build(),
            ObjectReference.builder().id("3").name("region2").type("findSpot").build()
        ))
        .build();
    }

    @Test
    void serviceInjected() {
        assertNotNull(service);
    }

    @Test
    void countRelations() {
        assertEquals(2, this.thsEntry.getRelations().get("partOf").size(), "predicate 'partOf' should have 2 values");
    }

    @Test
    void getRequestSingleEntry() throws Exception {
        when(repo.findById(anyString()))
            .thenReturn(
                Optional.of(thsEntry)
            );
        mockMvc.perform(
            get("/ths/get/xxx")
                .contentType("application/json")
        )
            .andExpect(
                status().isOk()
            )
            .andExpect(jsonPath("$.relations.partOf[0].id").value("2"));
    }

}