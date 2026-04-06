package com.finance.dashboard.auth.controller;

import com.finance.dashboard.auth.dto.LoginRequest;
import com.finance.dashboard.auth.dto.RegisterRequest;
import com.finance.dashboard.auth.model.RoleName;
import com.finance.dashboard.auth.model.User;
import com.finance.dashboard.auth.model.UserStatus;
import com.finance.dashboard.shared.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    // --- REGISTER ---

    @Test
    void register_withValidData_returns201AndTokens() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "user@test.com", "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400000));
    }

    @Test
    void register_withDuplicateEmail_returns400() throws Exception {
        createUser("user@test.com", "password123", RoleName.ROLE_VIEWER);

        RegisterRequest req = new RegisterRequest(
                "user@test.com", "password123", "Another User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void register_withBlankEmail_returns400WithValidationError() throws Exception {
        RegisterRequest req = new RegisterRequest("", "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.email").exists());
    }

    @Test
    void register_withShortPassword_returns400WithValidationError() throws Exception {
        RegisterRequest req = new RegisterRequest("user@test.com", "short", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void register_withInvalidEmailFormat_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("not-an-email", "password123", "Test");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").exists());
    }

    // --- LOGIN ---

    @Test
    void login_withValidCredentials_returns200AndTokens() throws Exception {
        createUser("user@test.com", "password123", RoleName.ROLE_VIEWER);

        LoginRequest req = new LoginRequest("user@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        createUser("user@test.com", "password123", RoleName.ROLE_VIEWER);

        LoginRequest req = new LoginRequest("user@test.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest("nobody@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withInactiveUser_returns401() throws Exception {
        User user = createUser("user@test.com", "password123", RoleName.ROLE_VIEWER);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        LoginRequest req = new LoginRequest("user@test.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- REFRESH ---

    @Test
    void refresh_withValidToken_returnsNewTokens() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "user@test.com", "password123", "Test User");

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(body)
                .get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void refresh_withInvalidToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", "totally-invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}