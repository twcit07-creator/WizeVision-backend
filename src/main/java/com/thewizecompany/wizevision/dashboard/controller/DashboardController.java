package com.thewizecompany.wizevision.dashboard.controller;

import com.thewizecompany.wizevision.dashboard.dto.AdminDashboardResponse;
import com.thewizecompany.wizevision.dashboard.dto.FinanceDashboardResponse;
import com.thewizecompany.wizevision.dashboard.dto.HrDashboardResponse;
import com.thewizecompany.wizevision.dashboard.dto.PmDashboardResponse;
import com.thewizecompany.wizevision.dashboard.service.DashboardService;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(
        name = "Dashboard",
        description = "Role-based dashboard summaries"
)
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin dashboard",
            description = "Complete overview: employees, projects, " +
                    "bids, financials, HR alerts."
    )
    public ResponseEntity<ApiResponse<AdminDashboardResponse>>
    adminDashboard() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        dashboardService.getAdminDashboard()
                )
        );
    }

    @GetMapping("/pm")
    @PreAuthorize(
            "hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')"
    )
    @Operation(
            summary = "Project Manager dashboard",
            description = "My projects, bids, inquiries, " +
                    "leave approvals."
    )
    public ResponseEntity<ApiResponse<PmDashboardResponse>>
    pmDashboard(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        dashboardService.getPmDashboard(
                                resolveEmployeeId(email)
                        )
                )
        );
    }

    @GetMapping("/hr")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "HR dashboard",
            description = "Headcount, leave summary, " +
                    "payroll status."
    )
    public ResponseEntity<ApiResponse<HrDashboardResponse>>
    hrDashboard() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        dashboardService.getHrDashboard()
                )
        );
    }

    @GetMapping("/finance")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')"
    )
    @Operation(
            summary = "Finance dashboard",
            description = "Revenue, outstanding, aging buckets, " +
                    "top clients."
    )
    public ResponseEntity<ApiResponse<FinanceDashboardResponse>>
    financeDashboard() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        dashboardService.getFinanceDashboard()
                )
        );
    }

    private UUID resolveEmployeeId(String email) {
        return employeeRepository
                .findByEmailAndIsDeletedFalseAndIsActiveTrue(email)
                .map(BaseEntity::getId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee", email
                        )
                );
    }
}