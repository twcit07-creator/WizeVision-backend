package com.thewizecompany.wizevision.employee.controller;

import com.thewizecompany.wizevision.employee.dto.CreateDepartmentRequest;
import com.thewizecompany.wizevision.employee.dto.DepartmentResponse;
import com.thewizecompany.wizevision.employee.service.DepartmentService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(
        name = "Department Management",
        description = "Manage company departments"
)
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create a new department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(
            @Valid @RequestBody CreateDepartmentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        departmentService.create(request),
                        "Department created successfully"
                ));
    }

    /*
     * Public to all authenticated users —
     * every employee selection form needs this list.
     */
    @GetMapping
    @Operation(summary = "Get all active departments")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>>
    getAll() {

        return ResponseEntity.ok(
                ApiResponse.ok(departmentService.getAllActive())
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<ApiResponse<DepartmentResponse>>
    getById(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(departmentService.getById(id))
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDepartmentRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        departmentService.update(id, request),
                        "Department updated successfully"
                )
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Deactivate a department")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id) {

        departmentService.deactivate(id);
        return ResponseEntity.ok(
                ApiResponse.ok("Department deactivated")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a department (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal String currentUserEmail) {

        departmentService.delete(id, currentUserEmail);
        return ResponseEntity.ok(
                ApiResponse.ok("Department deleted successfully")
        );
    }
}