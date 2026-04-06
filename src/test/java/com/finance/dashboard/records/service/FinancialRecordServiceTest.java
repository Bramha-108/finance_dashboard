package com.finance.dashboard.records.service;

import com.finance.dashboard.auth.model.RoleName;
import com.finance.dashboard.auth.model.User;
import com.finance.dashboard.records.dto.RecordFilterRequest;
import com.finance.dashboard.records.dto.RecordRequest;
import com.finance.dashboard.records.dto.RecordResponse;
import com.finance.dashboard.records.model.RecordCategory;
import com.finance.dashboard.records.model.RecordType;
import com.finance.dashboard.records.repository.FinancialRecordRepository;
import com.finance.dashboard.shared.BaseIntegrationTest;
import com.finance.dashboard.shared.TokenHelper;
import com.finance.dashboard.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinancialRecordServiceTest extends BaseIntegrationTest {

    @Autowired
    private FinancialRecordService recordService;

    @Autowired
    private FinancialRecordRepository recordRepo;

    private String analystToken;
    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() throws Exception {
        recordRepo.deleteAll();

        createUser("analyst@test.com", "password123",
                RoleName.ROLE_ANALYST, RoleName.ROLE_VIEWER);
        createUser("admin@test.com",   "password123", RoleName.ROLE_ADMIN);
        createUser("viewer@test.com",  "password123", RoleName.ROLE_VIEWER);

        analystToken = TokenHelper.getToken(mockMvc, objectMapper,
                "analyst@test.com", "password123");
        adminToken   = TokenHelper.getToken(mockMvc, objectMapper,
                "admin@test.com",   "password123");
        viewerToken  = TokenHelper.getToken(mockMvc, objectMapper,
                "viewer@test.com",  "password123");
    }

    // --- CREATE ---

    @Test
    void analyst_canCreateRecord_andItAppearsInTheirList() throws Exception {
        RecordRequest req = new RecordRequest(
                new BigDecimal("1000.00"), RecordType.INCOME,
                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), "Test income");

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1000.0))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.createdBy").value("analyst@test.com"));
    }

    @Test
    void viewer_cannotCreateRecord_gets403() throws Exception {
        RecordRequest req = new RecordRequest(
                new BigDecimal("500.00"), RecordType.EXPENSE,
                RecordCategory.FOOD, LocalDate.of(2025, 4, 1), null);

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequest_gets401() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isUnauthorized());
    }

    // --- OWNERSHIP ISOLATION ---

    @Test
    void analyst_onlySeesOwnRecords_notAdminsRecords() throws Exception {
        // Analyst creates one record
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1000.00"), RecordType.INCOME,
                                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated());

        // Admin creates one record
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("9999.00"), RecordType.INCOME,
                                RecordCategory.OTHER, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated());

        // Analyst listing must only show 1 record
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].createdBy").value("analyst@test.com"));
    }

    @Test
    void admin_seesAllRecords_fromAllUsers() throws Exception {
        // Both analyst and admin create records
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1000.00"), RecordType.INCOME,
                                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("500.00"), RecordType.EXPENSE,
                                RecordCategory.RENT, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated());

        // Admin sees both
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // --- UPDATE OWNERSHIP ---

    @Test
    void analyst_cannotUpdateAnotherUsersRecord_gets403() throws Exception {
        // Admin creates a record
        String body = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("9999.00"), RecordType.INCOME,
                                RecordCategory.OTHER, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String recordId = objectMapper.readTree(body).get("id").asText();

        // Analyst tries to update it
        mockMvc.perform(put("/api/records/" + recordId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1.00"), RecordType.EXPENSE,
                                RecordCategory.FOOD, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void analyst_canUpdateOwnRecord() throws Exception {
        String body = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1000.00"), RecordType.INCOME,
                                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String recordId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(put("/api/records/" + recordId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1500.00"), RecordType.INCOME,
                                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.0))
                .andExpect(jsonPath("$.notes").value("Updated"));
    }

    // --- SOFT DELETE ---

    @Test
    void softDelete_hidesRecordFromListing_butKeepsItInDatabase() throws Exception {
        String body = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1000.00"), RecordType.INCOME,
                                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String recordId = objectMapper.readTree(body).get("id").asText();

        // Delete it
        mockMvc.perform(delete("/api/records/" + recordId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isNoContent());

        // Listing returns 0
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Still exists in DB with deleted=true
        assertThat(recordRepo.findById(java.util.UUID.fromString(recordId)))
                .isPresent()
                .get()
                .extracting(r -> r.isDeleted())
                .isEqualTo(true);
    }

    @Test
    void getDeletedRecordById_returns404() throws Exception {
        String body = mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1000.00"), RecordType.INCOME,
                                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), null))))
                .andReturn().getResponse().getContentAsString();

        String recordId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(delete("/api/records/" + recordId)
                .header("Authorization", "Bearer " + analystToken));

        mockMvc.perform(get("/api/records/" + recordId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isNotFound());
    }

    // --- FILTERS ---

    @Test
    void filterByType_returnsOnlyMatchingRecords() throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("1000.00"), RecordType.INCOME,
                                RecordCategory.SALARY, LocalDate.of(2025, 4, 1), null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(
                                new BigDecimal("500.00"), RecordType.EXPENSE,
                                RecordCategory.RENT, LocalDate.of(2025, 4, 2), null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/records?type=INCOME")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("INCOME"));
    }
}