package dev.esgenius.controller;

import dev.esgenius.dto.*;
import dev.esgenius.service.CompanyEsgService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyEsgService companyEsgService;

    @Test
    void testListCompanies() throws Exception {
        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].ticker", notNullValue()))
                .andDo(print());
    }

    @Test
    void testGetCompanyEsgProfile() throws Exception {
        // Get first company ID from the list
        List<CompanySummaryResponse> companies = companyEsgService.listCompanies();
        Long companyId = companies.get(0).id();

        mockMvc.perform(get("/api/v1/companies/{companyId}/esg", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", notNullValue()))
                .andExpect(jsonPath("$.currentRating", notNullValue()))
                .andExpect(jsonPath("$.materialIssues", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.ratingHistory", hasSize(greaterThanOrEqualTo(1))))
                .andDo(print());
    }

    @Test
    void testCompareCompanies() throws Exception {
        // Get first two company IDs
        List<CompanySummaryResponse> companies = companyEsgService.listCompanies();
        Long companyA = companies.get(0).id();
        Long companyB = companies.get(1).id();

        mockMvc.perform(get("/api/v1/companies/compare")
                .param("companyA", companyA.toString())
                .param("companyB", companyB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyA", notNullValue()))
                .andExpect(jsonPath("$.companyB", notNullValue()))
                .andExpect(jsonPath("$.comparisonInsight", notNullValue()))
                .andDo(print());
    }

    @Test
    void testCompareCompanies_SameCompanyRejects() throws Exception {
        List<CompanySummaryResponse> companies = companyEsgService.listCompanies();
        Long companyId = companies.get(0).id();

        mockMvc.perform(get("/api/v1/companies/compare")
                .param("companyA", companyId.toString())
                .param("companyB", companyId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Cannot compare a company with itself")))
                .andDo(print());
    }

    @Test
    void testGetCompanyEsgProfile_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/companies/99999/esg"))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
