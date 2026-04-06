package com.finance.dashboard.shared.exception;

import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, String> details
) {}