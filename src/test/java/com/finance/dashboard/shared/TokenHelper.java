package com.finance.dashboard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.auth.dto.LoginRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TokenHelper {

    public static String getToken(MockMvc mockMvc,
                                  ObjectMapper mapper,
                                  String email,
                                  String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return mapper.readTree(body).get("accessToken").asText();
    }
}