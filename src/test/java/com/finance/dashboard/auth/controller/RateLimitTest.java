package com.finance.dashboard.auth.controller;

import com.finance.dashboard.auth.dto.LoginRequest;
import com.finance.dashboard.shared.security.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // This test uses the REAL RateLimitFilter — no override
    @Test
    void loginEndpoint_after5Requests_returns429() throws Exception {
        LoginRequest req = new LoginRequest("any@test.com", "wrongpassword");
        String body = objectMapper.writeValueAsString(req);

        // First 5 should pass through (401 because user doesn't exist, not 429)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(result ->
                            org.junit.jupiter.api.Assertions.assertNotEquals(
                                    429, result.getResponse().getStatus()));
        }

        // 6th request from same IP must be 429
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }
}