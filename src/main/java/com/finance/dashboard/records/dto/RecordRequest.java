package com.finance.dashboard.records.dto;

import com.finance.dashboard.records.model.RecordCategory;
import com.finance.dashboard.records.model.RecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordRequest(
        @Schema(description = "Amount — must be positive", example = "1500.00")
        @NotNull @Positive BigDecimal amount,

        @Schema(description = "INCOME or EXPENSE", example = "INCOME")
        @NotNull RecordType type,

        @Schema(description = "Category", example = "SALARY")
        RecordCategory category,

        @Schema(description = "Date of the record", example = "2025-04-01")
        @NotNull LocalDate date,

        @Schema(description = "Optional notes (max 500 chars)", example = "April salary payment")
        @Size(max = 500) String notes
) {}