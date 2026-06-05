package com.thewizecompany.wizevision.hr.controller;

import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.hr.dto.ApplyLeaveRequest;
import com.thewizecompany.wizevision.hr.dto.AssignSalaryStructureRequest;
import com.thewizecompany.wizevision.hr.dto.InitiatePayrollRequest;
import com.thewizecompany.wizevision.hr.dto.LeaveActionRequest;
import com.thewizecompany.wizevision.hr.dto.LeaveApplicationResponse;
import com.thewizecompany.wizevision.hr.dto.LeaveBalanceResponse;
import com.thewizecompany.wizevision.hr.dto.PayrollRunResponse;
import com.thewizecompany.wizevision.hr.dto.PayslipResponse;
import com.thewizecompany.wizevision.hr.repository.LeaveTypeRepository;
import com.thewizecompany.wizevision.hr.service.LeaveService;
import com.thewizecompany.wizevision.hr.service.PayrollService;
import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
@Tag(
        name = "HR",
        description = "Leave management and payroll"
)
@SecurityRequirement(name = "bearerAuth")
public class HrController {

    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    // ─────────────────────────────────────────────────────────
    // LEAVE TYPES (read-only for all)
    // ─────────────────────────────────────────────────────────

    @GetMapping("/leave-types")
    @Operation(summary = "Get all active leave types")
    public ResponseEntity<ApiResponse<?>> getLeaveTypes() {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveTypeRepository
                                .findByIsActiveTrueAndIsDeletedFalse()
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // LEAVE BALANCE
    // ─────────────────────────────────────────────────────────

    @GetMapping("/leave-balance/my")
    @Operation(summary = "My leave balance for current year")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>>
    getMyBalance(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.getMyBalances(
                                resolveEmployeeId(email)
                        )
                )
        );
    }

    @GetMapping("/leave-balance/{employeeId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "HR — Get leave balance for any employee"
    )
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>>
    getEmployeeBalance(
            @PathVariable UUID employeeId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.getMyBalances(employeeId)
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // LEAVE APPLICATIONS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/leave")
    @Operation(summary = "Apply for leave")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>>
    applyLeave(
            @Valid @RequestBody ApplyLeaveRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        leaveService.applyForLeave(
                                resolveEmployeeId(email), request
                        ),
                        "Leave application submitted"
                ));
    }

    @GetMapping("/leave/my")
    @Operation(summary = "My leave applications")
    public ResponseEntity<ApiResponse<PageResponse<LeaveApplicationResponse>>>
    getMyLeaves(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.getMyLeaves(
                                resolveEmployeeId(email),
                                PageRequest.of(
                                        page, size,
                                        Sort.by("createdAt").descending()
                                )
                        )
                )
        );
    }

    @PatchMapping("/leave/{id}/cancel")
    @Operation(summary = "Cancel my leave application")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>>
    cancelLeave(
            @PathVariable UUID id,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.cancelLeave(
                                id, resolveEmployeeId(email)
                        ),
                        "Leave application cancelled"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // TEAM LEAD ACTIONS
    // ─────────────────────────────────────────────────────────

    @GetMapping("/leave/pending-tl")
    @Operation(
            summary = "TL — Leaves pending my approval"
    )
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>>
    getPendingForTl(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.getPendingForTl(
                                resolveEmployeeId(email)
                        )
                )
        );
    }

    @PatchMapping("/leave/{id}/tl-action")
    @Operation(summary = "TL — Approve or reject leave")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>>
    tlAction(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveActionRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.tlAction(
                                id, resolveEmployeeId(email), request
                        ),
                        request.getAction().name()
                                .equals("APPROVE")
                                ? "Leave approved by TL"
                                : "Leave rejected by TL"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // PM ACTIONS
    // ─────────────────────────────────────────────────────────

    @GetMapping("/leave/pending-pm")
    @Operation(
            summary = "PM — Leaves pending my approval"
    )
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>>
    getPendingForPm(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.getPendingForPm(
                                resolveEmployeeId(email)
                        )
                )
        );
    }

    @PatchMapping("/leave/{id}/pm-action")
    @PreAuthorize(
            "hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')"
    )
    @Operation(summary = "PM — Approve or reject leave")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>>
    pmAction(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveActionRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.pmAction(
                                id, resolveEmployeeId(email), request
                        ),
                        request.getAction().name()
                                .equals("APPROVE")
                                ? "Leave approved by PM"
                                : "Leave rejected by PM"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // HR ACTIONS
    // ─────────────────────────────────────────────────────────

    @GetMapping("/leave/pending-hr")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(summary = "HR — All leaves pending final approval")
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>>
    getPendingForHr() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.getPendingForHr()
                )
        );
    }

    @PatchMapping("/leave/{id}/hr-action")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(summary = "HR — Final approval or rejection")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>>
    hrAction(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveActionRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        leaveService.hrAction(
                                id, resolveEmployeeId(email), request
                        ),
                        request.getAction().name()
                                .equals("APPROVE")
                                ? "Leave approved"
                                : "Leave rejected"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // SALARY STRUCTURE
    // ─────────────────────────────────────────────────────────

    @PostMapping("/salary-structure/{employeeId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "HR — Assign salary structure to employee",
            description = "Defines earnings and deductions " +
                    "for payroll calculation."
    )
    public ResponseEntity<ApiResponse<Void>>
    assignSalaryStructure(
            @PathVariable UUID employeeId,
            @Valid @RequestBody
            AssignSalaryStructureRequest request) {

        payrollService.assignSalaryStructure(
                employeeId, request
        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Salary structure assigned successfully"
                )
        );
    }

    @GetMapping("/salary-components")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "HR — Get all salary components",
            description = "Used when building salary structure form."
    )
    public ResponseEntity<ApiResponse<?>>
    getSalaryComponents() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        payrollService.getSalaryComponents()
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // PAYROLL
    // ─────────────────────────────────────────────────────────

    @PostMapping("/payroll/initiate")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "HR — Initiate payroll run for a month"
    )
    public ResponseEntity<ApiResponse<PayrollRunResponse>>
    initiatePayroll(
            @Valid @RequestBody
            InitiatePayrollRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        payrollService.initiatePayrollRun(
                                request, resolveEmployeeId(email)
                        ),
                        "Payroll run initiated"
                ));
    }

    @PatchMapping("/payroll/{runId}/process")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "HR — Process payroll (calculate all payslips)",
            description = "Calculates payslips for all active " +
                    "employees based on attendance and " +
                    "salary structures."
    )
    public ResponseEntity<ApiResponse<PayrollRunResponse>>
    processPayroll(@PathVariable UUID runId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        payrollService.processPayroll(runId),
                        "Payroll processed successfully"
                )
        );
    }

    @PatchMapping("/payroll/{runId}/finalize")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "HR — Finalize payroll",
            description = "Locks payslips. No changes allowed after."
    )
    public ResponseEntity<ApiResponse<PayrollRunResponse>>
    finalizePayroll(@PathVariable UUID runId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        payrollService.finalizePayroll(runId),
                        "Payroll finalized"
                )
        );
    }

    @GetMapping("/payroll")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(summary = "HR — Get all payroll runs")
    public ResponseEntity<ApiResponse<List<PayrollRunResponse>>>
    getAllRuns() {

        return ResponseEntity.ok(
                ApiResponse.ok(payrollService.getAllRuns())
        );
    }

    @GetMapping("/payslip/my")
    @Operation(summary = "My payslip history")
    public ResponseEntity<ApiResponse<List<PayslipResponse>>>
    getMyPayslips(
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        payrollService.getMyPayslips(
                                resolveEmployeeId(email)
                        )
                )
        );
    }

    @GetMapping("/payslip/my/{month}/{year}")
    @Operation(summary = "My payslip for a specific month")
    public ResponseEntity<ApiResponse<PayslipResponse>>
    getMyPayslip(
            @PathVariable Integer month,
            @PathVariable Integer year,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        payrollService.getMyPayslip(
                                resolveEmployeeId(email), month, year
                        )
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────

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