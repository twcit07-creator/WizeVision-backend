package com.thewizecompany.wizevision.projects.controller;

import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.projects.dto.ApproveChangeOrderRequest;
import com.thewizecompany.wizevision.projects.dto.AssignTeamRequest;
import com.thewizecompany.wizevision.projects.dto.ChangeOrderResponse;
import com.thewizecompany.wizevision.projects.dto.CreateChangeOrderRequest;
import com.thewizecompany.wizevision.projects.dto.ProjectResponse;
import com.thewizecompany.wizevision.projects.dto.UpdateProjectProgressRequest;
import com.thewizecompany.wizevision.projects.service.ProjectService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(
        name = "Projects",
        description = "Project management, team assignment, " +
                "progress tracking and change orders"
)
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;
    private final EmployeeRepository employeeRepository;

    // ─────────────────────────────────────────────────────────
    // CREATE FROM BID (Admin triggers this)
    // ─────────────────────────────────────────────────────────

    @PostMapping("/from-bid/{bidId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Create project from accepted bid",
            description = "Called after bid is ACCEPTED. " +
                    "Creates project with number J-TWC-{year}-{seq}."
    )
    public ResponseEntity<ApiResponse<ProjectResponse>>
    createFromBid(
            @PathVariable UUID bidId,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.createFromBid(
                                bidId, resolveEmployeeId(email)
                        ),
                        "Project created successfully"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER'," +
                    "'MODELER','EDITOR','CHECKER','HR_MANAGER','FINANCE')"
    )
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(
            @PathVariable UUID id, @AuthenticationPrincipal String email) {

        boolean showFinancials = isAdminOrFinance(email);

        return ResponseEntity.ok(
                ApiResponse.ok(projectService.getById(id, showFinancials))
        );
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER'," +
                    "'HR_MANAGER','FINANCE')"
    )
    @Operation(summary = "Search all projects")
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>>
    search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID pmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String email) {

        boolean showFinancials = isAdminOrFinance(email);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.search(
                                search, status, clientId, pmId,
                                PageRequest.of(page, size),
                                showFinancials
                        )
                )
        );
    }

    @GetMapping("/my")
    @Operation(
            summary = "Get my active projects",
            description = "Returns projects where the current user " +
                    "is PM, Modeler, Editor, or Checker."
    )
    public ResponseEntity<ApiResponse<List<ProjectResponse>>>
    getMyProjects(
            @AuthenticationPrincipal String email) {

        boolean showFinancials = isAdminOrFinance(email);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.getMyProjects(
                                resolveEmployeeId(email),
                                showFinancials
                        )
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // TEAM ASSIGNMENT (PM)
    // ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/assign-team")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(
            summary = "PM — Assign team members",
            description = "Assigns Modeler, Editor, and Checker. " +
                    "Project moves from PLANNING to ACTIVE " +
                    "when all three are assigned."
    )
    public ResponseEntity<ApiResponse<ProjectResponse>>
    assignTeam(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTeamRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.assignTeam(
                                id, request, email
                        ),
                        "Team assigned. Project is now ACTIVE."
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // PROGRESS (PM)
    // ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/progress")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(summary = "PM — Update project progress and phase")
    public ResponseEntity<ApiResponse<ProjectResponse>>
    updateProgress(
            @PathVariable UUID id,
            @Valid @RequestBody
            UpdateProjectProgressRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.updateProgress(
                                id, request,
                                resolveEmployeeId(email)
                        ),
                        "Progress updated"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // STATUS CHANGES
    // ─────────────────────────────────────────────────────────

    @PatchMapping("/{id}/hold")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(summary = "Put project on hold")
    public ResponseEntity<ApiResponse<ProjectResponse>> onHold(
            @PathVariable UUID id,
            @RequestParam String reason) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.putOnHold(id, reason),
                        "Project put on hold"
                )
        );
    }

    @PatchMapping("/{id}/resume")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(summary = "Resume a project on hold")
    public ResponseEntity<ApiResponse<ProjectResponse>> resume(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.resume(id),
                        "Project resumed"
                )
        );
    }

    @PatchMapping("/{id}/deliver")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(summary = "Mark project as delivered to client")
    public ResponseEntity<ApiResponse<ProjectResponse>> deliver(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.markDelivered(id),
                        "Project marked as delivered"
                )
        );
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Mark project as completed",
            description = "Client has approved. Project is closed."
    )
    public ResponseEntity<ApiResponse<ProjectResponse>> complete(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.complete(id),
                        "Project completed successfully"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // CHANGE ORDERS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/change-orders")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(
            summary = "PM — Create change order",
            description = "Number format: " +
                    "{projectNumber}-COR-{seq}. " +
                    "Example: J-TWC-2026-001-COR-001"
    )
    public ResponseEntity<ApiResponse<ChangeOrderResponse>>
    createChangeOrder(
            @PathVariable UUID id,
            @Valid @RequestBody
            CreateChangeOrderRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.createChangeOrder(
                                id, request, resolveEmployeeId(email)
                        ),
                        "Change order created"
                )
        );
    }

    @PatchMapping("/{id}/change-orders/{coId}/submit")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(summary = "PM — Submit change order to admin")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>>
    submitChangeOrder(
            @PathVariable UUID id,
            @PathVariable UUID coId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.submitChangeOrder(id, coId),
                        "Change order submitted"
                )
        );
    }

    @PatchMapping("/{id}/change-orders/{coId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Approve change order with amount",
            description = "Adds amount to project's total " +
                    "contract value."
    )
    public ResponseEntity<ApiResponse<ChangeOrderResponse>>
    approveChangeOrder(
            @PathVariable UUID id,
            @PathVariable UUID coId,
            @Valid @RequestBody
            ApproveChangeOrderRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.approveChangeOrder(
                                id, coId, request,
                                resolveEmployeeId(email)
                        ),
                        "Change order approved"
                )
        );
    }

    @PatchMapping("/{id}/change-orders/{coId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Admin — Reject change order")
    public ResponseEntity<ApiResponse<ChangeOrderResponse>>
    rejectChangeOrder(
            @PathVariable UUID id,
            @PathVariable UUID coId,
            @RequestParam String reason,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.rejectChangeOrder(
                                id, coId, reason,
                                resolveEmployeeId(email)
                        ),
                        "Change order rejected"
                )
        );
    }

    @GetMapping("/{id}/change-orders")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER'," +
                    "'FINANCE')"
    )
    @Operation(summary = "Get all change orders for a project")
    public ResponseEntity<ApiResponse<List<ChangeOrderResponse>>>
    getChangeOrders(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        projectService.getChangeOrders(id)
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

    /*
     * helper to Control The Amount Field Visibility to all employees other than Admin and Super Admin:
     */
    private boolean isAdminOrFinance(String email) {
        return employeeRepository
                .findByEmailAndIsDeletedFalseAndIsActiveTrue(email)
                .map(e -> e.getRole()
                        == com.thewizecompany.wizevision
                        .employee.domain.Role.SUPER_ADMIN
                        || e.getRole()
                        == com.thewizecompany.wizevision
                        .employee.domain.Role.ADMIN
                        || e.getRole()
                        == com.thewizecompany.wizevision
                        .employee.domain.Role.FINANCE
                )
                .orElse(false);
    }
}