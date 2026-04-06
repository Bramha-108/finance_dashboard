package com.finance.dashboard.records.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import com.finance.dashboard.records.model.RecordType;
import com.finance.dashboard.records.model.RecordCategory;

public record RecordFilterRequest(
        @Schema(description = "Start date", example = "2025-04-01", type = "string", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

        @Schema(description = "End date", example = "2025-04-30", type = "string", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

        @Schema(description = "INCOME or EXPENSE")
        RecordType type,

        @Schema(description = "Category filter")
        RecordCategory category,

        @Schema(description = "Page number (zero-based)", defaultValue = "0")
        Integer page,

        @Schema(description = "Page size", defaultValue = "20")
        Integer size
) {
    public RecordFilterRequest {
        if (page == null || page < 0) page = 0;
        if (size == null || size <= 0) size = 20;
        if (size > 100) size = 100;
    }

    public int getPage() { return page == null ? 0 : page; }
    public int getSize() { return size == null ? 20 : size; }
}