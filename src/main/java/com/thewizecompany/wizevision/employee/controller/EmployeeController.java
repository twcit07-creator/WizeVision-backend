package com.thewizecompany.wizevision.employee.controller;

import com.thewizecompany.wizevision.employee.dto.*;
import com.thewizecompany.wizevision.employee.service.EmployeeService;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/*
 * EMPLOYEE CONTROLLER
 *
 * Thin controller — validates input, calls service, returns response.
 *
 * AUTHORIZATION STRATEGY:
 * @PreAuthorize on each method controls who can do what.
 *
 * CREATE employee:  ADMIN, HR_MANAGER, SUPER_ADMIN
 * READ employees:   ADMIN, HR_MANAGER, SUPER_ADMIN, PROJECT_MANAGER
 * UPDATE employee:  ADMIN, HR_MANAGER, SUPER_ADMIN
 * DISABLE employee: ADMIN, HR_MANAGER, SUPER_ADMIN
 * DELETE employee:  ADMIN, SUPER_ADMIN only
 *
 * @AuthenticationPrincipal String email
 * Injects the currently logged-in user's email.
 * This is the principal set in JwtAuthFilter.
 * Used for audit trails — "deleted by: admin@wizevision.com"
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(
        name = "Employee Management",
        description = "Create and manage employee accounts"
)
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Create a new employee account")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody CreateEmployeeRequest request) {

        EmployeeResponse response =
                employeeService.createEmployee(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        response,
                        "Employee created successfully"
                ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN'," +
            "'HR_MANAGER','PROJECT_MANAGER')")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(employeeService.getById(id))
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN'," +
            "'HR_MANAGER','PROJECT_MANAGER')")
    @Operation(summary = "Get all employees (paginated)")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>>
    getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt")
            String sortBy,
            @RequestParam(defaultValue = "desc")
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        employeeService.getAllEmployees(pageable)
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Update employee details")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        employeeService.updateEmployee(id, request),
                        "Employee updated successfully"
                )
        );
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Disable an employee account")
    public ResponseEntity<ApiResponse<Void>> disable(
            @PathVariable UUID id) {

        employeeService.disableEmployee(id);

        return ResponseEntity.ok(
                ApiResponse.ok("Employee disabled successfully")
        );
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Re-enable a disabled employee account")
    public ResponseEntity<ApiResponse<Void>> enable(
            @PathVariable UUID id) {

        employeeService.enableEmployee(id);

        return ResponseEntity.ok(
                ApiResponse.ok("Employee enabled successfully")
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Soft delete an employee account")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal String currentUserEmail) {

        employeeService.deleteEmployee(id, currentUserEmail);

        return ResponseEntity.ok(
                ApiResponse.ok("Employee deleted successfully")
        );
    }

    @PatchMapping("/{id}/attendance-pin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Set attendance PIN for Windows app login")
    public ResponseEntity<ApiResponse<Void>> setAttendancePin(
            @PathVariable UUID id,
            @Valid @RequestBody AttendancePinRequest request,
            @AuthenticationPrincipal String currentUserEmail) {

        employeeService.setAttendancePin(
                id,
                request,
                currentUserEmail
        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Attendance PIN set successfully"
                )
        );
    }

    @DeleteMapping("/{id}/attendance-pin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')")
    @Operation(summary = "Reset attendance PIN")
    public ResponseEntity<ApiResponse<Void>> resetAttendancePin(
            @PathVariable UUID id,
            @AuthenticationPrincipal String currentUserEmail) {

        employeeService.resetAttendancePin(id, currentUserEmail);

        return ResponseEntity.ok(
                ApiResponse.ok("Attendance PIN reset successfully")
        );
    }

    @GetMapping("/pm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'HR_MANAGER', 'MARKETING_EXECUTIVE')")
    public ResponseEntity<ApiResponse<List<ProjectManagerListResponse>>> getProjectManagerList(){
        return ResponseEntity.ok(
                ApiResponse.ok(
                        employeeService.getProjectManagerList()
                )
        );
    }
}