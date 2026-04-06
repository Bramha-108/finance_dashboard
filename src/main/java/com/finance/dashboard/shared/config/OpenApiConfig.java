package com.finance.dashboard.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Dashboard API")
                        .description("""
                    REST API for managing financial records with role-based access control.
                    
                    ## Authentication
                    All protected endpoints require a Bearer JWT token in the Authorization header.
                    Use the `/api/auth/login` endpoint to obtain a token, then click
                    **Authorize** and enter: `Bearer <your_token>`
                    
                    ## Roles
                    - **VIEWER** — read their own records only
                    - **ANALYST** — create, edit, delete own records + dashboard analytics
                    - **ADMIN** — full access to all records and all users
                    
                    ## Rate Limiting
                    Auth endpoints are limited to 5 requests per minute per IP.
                    """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Finance Dashboard")
                                .email("dev@finance.com"))
                        .license(new License()
                                .name("MIT License")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.finance.com").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token. Obtain it from POST /api/auth/login")));
    }
}