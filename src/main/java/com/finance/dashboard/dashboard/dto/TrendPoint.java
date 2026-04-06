package com.finance.dashboard.dashboard.dto;

import com.finance.dashboard.records.model.RecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Monthly trend data point")
public record TrendPoint(
        @Schema(description = "Year", example = "2025")
        int year,

        @Schema(description = "Month (1-12)", example = "4")
        int month,

        @Schema(description = "Record type - INCOME or EXPENSE")
        RecordType type,

        @Schema(description = "Total amount for this month and type")
        BigDecimal amount
) {}