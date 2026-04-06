package com.finance.dashboard.records.controller;

import com.finance.dashboard.records.dto.*;
import com.finance.dashboard.records.service.FinancialRecordService;
import com.finance.dashboard.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "Financial Records", description = "Create, view, update, and delete financial records")
@SecurityRequirement(name = "Bearer Authentication")
public class FinancialRecordController {

    private final FinancialRecordService service;

    @Operation(
            summary = "List records",
            description = """
            Returns a paginated list of financial records.
            - VIEWER and ANALYST see only their own records.
            - ADMIN sees all records from all users.
            All filter parameters are optional.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Records returned successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    public ResponseEntity<PagedResponse<RecordResponse>> list(
            @Parameter(description = "Filter start date (yyyy-MM-dd)") RecordFilterRequest filter,
            Authentication auth) {
        Page<RecordResponse> page = service.findAll(filter, auth);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @Operation(summary = "Get record by ID",
            description = "Returns a single record. Users can only access their own records. Admins can access any.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Record found"),
            @ApiResponse(responseCode = "403", description = "Not your record"),
            @ApiResponse(responseCode = "404", description = "Record not found or deleted")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','ANALYST','ADMIN')")
    public ResponseEntity<RecordResponse> getOne(
            @Parameter(description = "Record UUID") @PathVariable UUID id,
            Authentication auth) {
        return ResponseEntity.ok(service.findById(id, auth));
    }

    @Operation(summary = "Create a record",
            description = "Creates a new financial record owned by the authenticated user. ANALYST or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Record created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role — VIEWER cannot create records")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<RecordResponse> create(
            @Valid @RequestBody RecordRequest req,
            Authentication auth) {
        return ResponseEntity.status(201).body(service.create(req, auth));
    }

    @Operation(summary = "Update a record",
            description = "Updates an existing record. Users can only update their own records. Admins can update any.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Record updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Not your record or insufficient role"),
            @ApiResponse(responseCode = "404", description = "Record not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<RecordResponse> update(
            @Parameter(description = "Record UUID") @PathVariable UUID id,
            @Valid @RequestBody RecordRequest req,
            Authentication auth) {
        return ResponseEntity.ok(service.update(id, req, auth));
    }

    @Operation(summary = "Delete a record",
            description = "Soft-deletes a record. The record is hidden from all API responses but preserved in the database for analytics history.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Record deleted"),
            @ApiResponse(responseCode = "403", description = "Not your record or insufficient role"),
            @ApiResponse(responseCode = "404", description = "Record not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Record UUID") @PathVariable UUID id,
            Authentication auth) {
        service.softDelete(id, auth);
        return ResponseEntity.noContent().build();
    }
}