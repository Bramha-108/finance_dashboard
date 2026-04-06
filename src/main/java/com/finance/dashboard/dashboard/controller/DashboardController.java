package com.finance.dashboard.dashboard.controller;

import com.finance.dashboard.dashboard.dto.DashboardSummaryResponse;
import com.finance.dashboard.dashboard.dto.TrendPoint;
import com.finance.dashboard.dashboard.service.DashboardService;
import com.finance.dashboard.records.dto.RecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
@Tag(name = "Dashboard Analytics",
        description = "Aggregated financial summaries. ANALYST sees their own data. ADMIN sees all users' data.")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Full dashboard summary",
            description = """
            Returns a complete snapshot: total income, total expenses, net balance,
            category breakdown, 6-month monthly trend, and the 10 most recent records.
            Date range defaults to current month if not provided.
            """)
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary(
            @Parameter(description = "Start date (yyyy-MM-dd). Defaults to first day of current month.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (yyyy-MM-dd). Defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        return ResponseEntity.ok(dashboardService.getSummary(effectiveFrom, effectiveTo, auth));
    }

    @Operation(
            summary = "Category breakdown",
            description = "Returns total amount per category within the given date range.")
    @GetMapping("/categories")
    public ResponseEntity<Map<String, BigDecimal>> categories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        return ResponseEntity.ok(dashboardService.getCategoryBreakdown(effectiveFrom, effectiveTo, auth));
    }

    @Operation(
            summary = "Monthly trend",
            description = "Returns monthly income and expense totals as a time series. Defaults to last 6 months.")
    @GetMapping("/trend")
    public ResponseEntity<List<TrendPoint>> trend(
            @Parameter(description = "Start date of trend window. Defaults to 6 months ago.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
            Authentication auth) {
        LocalDate effectiveSince = since != null ? since : LocalDate.now().minusMonths(6);
        return ResponseEntity.ok(dashboardService.getMonthlyTrend(effectiveSince, auth));
    }

    @Operation(
            summary = "Recent activity",
            description = "Returns the 10 most recently created records for the current user (or all users for admin).")
    @GetMapping("/recent")
    public ResponseEntity<List<RecordResponse>> recent(Authentication auth) {
        return ResponseEntity.ok(dashboardService.getRecent(auth));
    }
}