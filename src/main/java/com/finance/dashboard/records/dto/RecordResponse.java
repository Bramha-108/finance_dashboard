package com.finance.dashboard.records.dto;

import com.finance.dashboard.records.model.RecordCategory;
import com.finance.dashboard.records.model.RecordType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecordResponse(
        UUID id,
        BigDecimal amount,
        RecordType type,
        RecordCategory category,
        LocalDate date,
        String notes,
        String createdBy,
        LocalDateTime createdAt
) {}
