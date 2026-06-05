package com.thewizecompany.wizevision.employee.controller;

import com.thewizecompany.wizevision.employee.dto.CreateDesignationRequest;
import com.thewizecompany.wizevision.employee.dto.DesignationResponse;
import com.thewizecompany.wizevision.employee.service.DesignationService;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/designations")
@RequiredArgsConstructor
@Tag(
        name = "Designation Management",
        description = "Manage job designations/titles"
)
@SecurityRequirement(name = "bearerAuth")
public class DesignationController {

    private final DesignationService designationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Create a new designation")
    public ResponseEntity<ApiResponse<DesignationResponse>> create(
            @Valid @RequestBody CreateDesignationRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        designationService.create(request),
                        "Designation created successfully"
                ));
    }

    @GetMapping
    @Operation(summary = "Get all active designations")
    public ResponseEntity<ApiResponse<List<DesignationResponse>>>
    getAll(
            @RequestParam(required = false) UUID departmentId) {

        /*
         * If departmentId is provided, filter by department.
         * Used by frontend: when user selects a department
         * in employee form, designations dropdown filters
         * to show only that department's titles.
         *
         * If no departmentId, returns all designations.
         */
        List<DesignationResponse> result = departmentId != null
                ? designationService.getByDepartment(departmentId)
                : designationService.getAllActive();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get designation by ID")
    public ResponseEntity<ApiResponse<DesignationResponse>>
    getById(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(designationService.getById(id))
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Deactivate a designation")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id) {

        designationService.deactivate(id);
        return ResponseEntity.ok(
                ApiResponse.ok("Designation deactivated")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Delete a designation (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal String currentUserEmail) {

        designationService.delete(id, currentUserEmail);
        return ResponseEntity.ok(
                ApiResponse.ok("Designation deleted successfully")
        );
    }
}