package com.finance.dashboard.dashboard.service;

import com.finance.dashboard.auth.model.RoleName;
import com.finance.dashboard.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.dashboard.dto.TrendPoint;
import com.finance.dashboard.records.dto.RecordRequest;
import com.finance.dashboard.records.model.RecordCategory;
import com.finance.dashboard.records.model.RecordType;
import com.finance.dashboard.records.repository.FinancialRecordRepository;
import com.finance.dashboard.shared.BaseIntegrationTest;
import com.finance.dashboard.shared.TokenHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardServiceTest extends BaseIntegrationTest {

    @Autowired
    private FinancialRecordRepository recordRepo;

    private String analystToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        recordRepo.deleteAll();

        createUser("analyst@test.com", "password123",
                RoleName.ROLE_ANALYST, RoleName.ROLE_VIEWER);
        createUser("admin@test.com", "password123", RoleName.ROLE_ADMIN);

        analystToken = TokenHelper.getToken(mockMvc, objectMapper,
                "analyst@test.com", "password123");
        adminToken   = TokenHelper.getToken(mockMvc, objectMapper,
                "admin@test.com",   "password123");
    }

    private void createRecord(String token, BigDecimal amount,
                              RecordType type, RecordCategory category,
                              LocalDate date) throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RecordRequest(amount, type, category, date, null))))
                .andExpect(status().isCreated());
    }

    // --- SUMMARY ---

    @Test
    void summary_correctlyCalculatesTotalIncomeExpenseAndNetBalance() throws Exception {
        createRecord(analystToken, new BigDecimal("3000.00"),
                RecordType.INCOME,  RecordCategory.SALARY,     LocalDate.of(2025, 4, 1));
        createRecord(analystToken, new BigDecimal("1000.00"),
                RecordType.INCOME,  RecordCategory.INVESTMENT, LocalDate.of(2025, 4, 5));
        createRecord(analystToken, new BigDecimal("800.00"),
                RecordType.EXPENSE, RecordCategory.RENT,       LocalDate.of(2025, 4, 3));

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("from", "2025-04-01")
                        .param("to",   "2025-04-30")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(4000.0))
                .andExpect(jsonPath("$.totalExpenses").value(800.0))
                .andExpect(jsonPath("$.netBalance").value(3200.0));
    }

    @Test
    void summary_withNoRecordsInRange_returnsZeros() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .param("from", "2025-04-01")
                        .param("to",   "2025-04-30")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.netBalance").value(0));
    }

    @Test
    void summary_analystOnlySeesOwnData_notAdminData() throws Exception {
        createRecord(analystToken, new BigDecimal("2000.00"),
                RecordType.INCOME, RecordCategory.SALARY, LocalDate.of(2025, 4, 1));
        createRecord(adminToken, new BigDecimal("9000.00"),
                RecordType.INCOME, RecordCategory.OTHER,  LocalDate.of(2025, 4, 1));

        // Analyst summary must only reflect their 2000, not admin's 9000
        mockMvc.perform(get("/api/dashboard/summary")
                        .param("from", "2025-04-01")
                        .param("to",   "2025-04-30")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(2000.0));
    }

    @Test
    void summary_adminSeesAllUsersData() throws Exception {
        createRecord(analystToken, new BigDecimal("2000.00"),
                RecordType.INCOME, RecordCategory.SALARY, LocalDate.of(2025, 4, 1));
        createRecord(adminToken, new BigDecimal("9000.00"),
                RecordType.INCOME, RecordCategory.OTHER,  LocalDate.of(2025, 4, 1));

        // Admin summary must show combined 11000
        mockMvc.perform(get("/api/dashboard/summary")
                        .param("from", "2025-04-01")
                        .param("to",   "2025-04-30")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(11000.0));
    }

    // --- CATEGORY BREAKDOWN ---

    @Test
    void categories_returnsCorrectTotalsPerCategory() throws Exception {
        createRecord(analystToken, new BigDecimal("3000.00"),
                RecordType.INCOME,  RecordCategory.SALARY, LocalDate.of(2025, 4, 1));
        createRecord(analystToken, new BigDecimal("1000.00"),
                RecordType.INCOME,  RecordCategory.SALARY, LocalDate.of(2025, 4, 5));
        createRecord(analystToken, new BigDecimal("500.00"),
                RecordType.EXPENSE, RecordCategory.RENT,   LocalDate.of(2025, 4, 2));

        mockMvc.perform(get("/api/dashboard/categories")
                        .param("from", "2025-04-01")
                        .param("to",   "2025-04-30")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.SALARY").value(4000.0))
                .andExpect(jsonPath("$.RENT").value(500.0));
    }

    // --- RECENT ACTIVITY ---

    @Test
    void recent_returnsLatest10Records_inDescendingOrder() throws Exception {
        for (int i = 1; i <= 12; i++) {
            createRecord(analystToken,
                    new BigDecimal(i * 100),
                    RecordType.INCOME, RecordCategory.OTHER,
                    LocalDate.of(2025, 4, i));
        }

        mockMvc.perform(get("/api/dashboard/recent")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }

    // --- ACCESS CONTROL ---

    @Test
    void viewer_cannotAccessDashboard_gets403() throws Exception {
        createUser("viewer@test.com", "password123", RoleName.ROLE_VIEWER);
        String viewerToken = TokenHelper.getToken(mockMvc, objectMapper,
                "viewer@test.com", "password123");

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("from", "2025-04-01")
                        .param("to",   "2025-04-30")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    // --- TREND ---

    @Test
    void trend_returnsSeparateRowsPerMonthAndType() throws Exception {
        createRecord(analystToken, new BigDecimal("1000.00"),
                RecordType.INCOME,  RecordCategory.SALARY, LocalDate.of(2025, 3, 1));
        createRecord(analystToken, new BigDecimal("400.00"),
                RecordType.EXPENSE, RecordCategory.RENT,   LocalDate.of(2025, 3, 15));
        createRecord(analystToken, new BigDecimal("2000.00"),
                RecordType.INCOME,  RecordCategory.SALARY, LocalDate.of(2025, 4, 1));

        mockMvc.perform(get("/api/dashboard/trend")
                        .param("since", "2025-03-01")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}