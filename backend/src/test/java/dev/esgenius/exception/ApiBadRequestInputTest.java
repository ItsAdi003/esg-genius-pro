package dev.esgenius.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiBadRequestInputTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void missingOrganizationIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("organizationId")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void nonNumericDocumentIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/documents/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("documentId")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void missingCompanyBReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/companies/compare").param("companyA", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("companyB")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
