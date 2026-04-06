package com.finance.dashboard.auth.dto;

import com.finance.dashboard.auth.model.UserStatus;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserStatus status,
        Set<String> roles,
        LocalDateTime createdAt
) {}