package dev.esgenius.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrameworkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listFrameworksReturnsArray() throws Exception {
        mockMvc.perform(get("/api/v1/frameworks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].code").value("BRSR"))
                .andExpect(jsonPath("$[0].requirementCount").value(14));
    }

    @Test
    void getFrameworkByIdReturnsDetails() throws Exception {
        // First get the list to find the BRSR ID
        String body = mockMvc.perform(get("/api/v1/frameworks"))
                .andReturn().getResponse().getContentAsString();

        // The BRSR framework is the first (and only) one seeded
        mockMvc.perform(get("/api/v1/frameworks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BRSR"))
                .andExpect(jsonPath("$.name").value("SEBI BRSR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getFrameworkReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/v1/frameworks/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void getRequirementsReturnsAll14() throws Exception {
        mockMvc.perform(get("/api/v1/frameworks/1/requirements"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[0].requirementCode").exists())
                .andExpect(jsonPath("$[0].category").exists())
                .andExpect(jsonPath("$[0].mandatory").exists());
    }

    @Test
    void getRequirementsFiltersByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/frameworks/1/requirements")
                        .param("category", "ENVIRONMENTAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].category", everyItem(is("ENVIRONMENTAL"))));
    }

    @Test
    void getRequirementsReturns404ForUnknownFramework() throws Exception {
        mockMvc.perform(get("/api/v1/frameworks/99999/requirements"))
                .andExpect(status().isNotFound());
    }
}
