package com.finance.dashboard.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.finance.dashboard.records.dto.RecordResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Dashboard summary response")
public record DashboardSummaryResponse(
        @Schema(description = "Total income")              BigDecimal totalIncome,
        @Schema(description = "Total expenses")           BigDecimal totalExpenses,
        @Schema(description = "Net balance")              BigDecimal netBalance,
        @Schema(description = "Breakdown by category")    Map<String, BigDecimal> byCategory,
        @Schema(description = "Monthly trend points")     List<TrendPoint> monthlyTrend,
        @Schema(description = "Recent transactions")      List<RecordResponse> recentActivity
) {}