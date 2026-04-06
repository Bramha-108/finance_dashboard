package com.finance.dashboard.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse of(String access, String refresh, long expiresIn) {
        return new TokenResponse(access, refresh, "Bearer", expiresIn);
    }
}