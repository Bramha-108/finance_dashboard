package com.finance.dashboard.auth.controller;

import com.finance.dashboard.auth.dto.UserResponse;
import com.finance.dashboard.auth.model.RoleName;
import com.finance.dashboard.auth.model.UserStatus;
import com.finance.dashboard.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Management", description = "Admin-only endpoints for managing users and roles")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users", description = "Returns all registered users. Admin only.")
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Set user status", description = "Activate or deactivate a user account. Admin only.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> setStatus(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Parameter(description = "ACTIVE or INACTIVE") @RequestParam UserStatus status) {
        return ResponseEntity.ok(userService.setStatus(id, status));
    }

    @Operation(summary = "Assign role to user", description = "Add a role to a user. Admin only.")
    @PostMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRole(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Parameter(description = "Role name e.g. ROLE_ANALYST") @RequestParam RoleName role) {
        return ResponseEntity.ok(userService.assignRole(id, role));
    }

    @Operation(summary = "Revoke role from user", description = "Remove a role from a user. Admin only.")
    @DeleteMapping("/{id}/roles/{role}")
    public ResponseEntity<UserResponse> revokeRole(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Parameter(description = "Role name to revoke") @PathVariable RoleName role) {
        return ResponseEntity.ok(userService.revokeRole(id, role));
    }
}