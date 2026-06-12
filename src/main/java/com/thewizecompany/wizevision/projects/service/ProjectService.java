package com.thewizecompany.wizevision.projects.service;

import com.thewizecompany.wizevision.bidding.domain.BidStatus;
import com.thewizecompany.wizevision.bidding.repository.BidRepository;
import com.thewizecompany.wizevision.client.domain.ClientContact;
import com.thewizecompany.wizevision.client.repository.ClientContactRepository;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.domain.Role;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.marketing.domain.InquiryStatus;
import com.thewizecompany.wizevision.marketing.repository.ProjectInquiryRepository;
import com.thewizecompany.wizevision.projects.domain.*;
import com.thewizecompany.wizevision.projects.dto.ApproveChangeOrderRequest;
import com.thewizecompany.wizevision.projects.dto.AssignTeamRequest;
import com.thewizecompany.wizevision.projects.dto.ChangeOrderResponse;
import com.thewizecompany.wizevision.projects.dto.CreateChangeOrderRequest;
import com.thewizecompany.wizevision.projects.dto.ProjectResponse;
import com.thewizecompany.wizevision.projects.dto.UpdateProjectProgressRequest;
import com.thewizecompany.wizevision.projects.repository.ChangeOrderRepository;
import com.thewizecompany.wizevision.projects.repository.ProjectAssignmentRepository;
import com.thewizecompany.wizevision.projects.repository.ProjectRepository;
import com.thewizecompany.wizevision.shared.domain.SequenceType;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import com.thewizecompany.wizevision.shared.service.SequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final ChangeOrderRepository changeOrderRepository;
    private final BidRepository bidRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectInquiryRepository inquiryRepository;
    private final SequenceService sequenceService;

    // ─────────────────────────────────────────────────────────
    // CREATE PROJECT FROM ACCEPTED BID
    // ─────────────────────────────────────────────────────────

    /*
     * Called automatically when Admin marks a bid as ACCEPTED.
     * Also called from BidService.recordDecision().
     *
     * This method is the bridge between Bidding and Projects.
     */
    @Transactional
    public ProjectResponse createFromBid(UUID bidId,
                                         UUID adminId) {

        var bid = bidRepository
                .findByIdAndIsDeletedFalse(bidId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bid", bidId.toString()
                        )
                );

        if (bid.getStatus() != BidStatus.ACCEPTED) {
            throw new BusinessException(
                    "Bid must be ACCEPTED to create a project",
                    "BID_NOT_ACCEPTED"
            );
        }

        /*
         * Check project does not already exist for this bid.
         * Prevents duplicate creation if called twice.
         */
        if (projectRepository
                .findByBidIdAndIsDeletedFalse(bidId)
                .isPresent()) {
            throw new BusinessException(
                    "Project already exists for this bid",
                    "PROJECT_ALREADY_EXISTS"
            );
        }

        String projectNumber = generateProjectNumber();

        Project project = Project.builder()
                .projectNumber(projectNumber)
                .bidId(bidId)
                .clientId(bid.getClientId())
                .clientContactId(bid.getClientContactId())
                .projectName(bid.getProjectName())
                .projectLocation(bid.getProjectLocation())
                .scopeOfWork(bid.getScopeOfWork())
                .inclusions(bid.getInclusions())
                .exclusions(bid.getExclusions())
                .contractAmount(bid.getBidAmount())
                .pmId(bid.getCreatedByPmId())
                .status(ProjectStatus.PLANNING)
                .currentPhase(ProjectPhase.MODELLING)
                .progressPercentage(0)
                .estimatedStartDate(bid.getProposedStartDate())
                .estimatedEndDate(bid.getProposedEndDate())
                .estimatedWeeks(bid.getEstimatedWeeks())
                .build();

        Project saved = projectRepository.save(project);

        /*
         * Link project back to bid.
         */
        bid.setConvertedProjectId(saved.getId());
        bidRepository.save(bid);

        /*
         * Close the inquiry if this came from one.
         */
        if (bid.getInquiryId() != null) {
            inquiryRepository
                    .findByIdAndIsDeletedFalse(bid.getInquiryId())
                    .ifPresent(inquiry -> {
                        inquiry.setStatus(InquiryStatus.CLOSED);
                        inquiryRepository.save(inquiry);
                    });
        }

        log.info(
                "Project created: {} from bid: {}",
                saved.getProjectNumber(),
                bid.getBidNumber()
        );

        return mapToResponse(saved, false);
    }

    // ─────────────────────────────────────────────────────────
    // ASSIGN TEAM
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse assignTeam(
            UUID projectId,
            AssignTeamRequest request,
            String assignedBy) {

        Project project = findProject(projectId);

        if (project.getStatus() == ProjectStatus.COMPLETED
                || project.getStatus()
                == ProjectStatus.CANCELLED) {
            throw new BusinessException(
                    "Cannot assign team to a closed project",
                    "PROJECT_CLOSED"
            );
        }

        for (var member : request.getMembers()) {

            /*
             * Validate employee exists and role matches.
             * TEAM_LEAD can be any senior employee —
             * no strict role check for TL.
             */
            var employee = employeeRepository
                    .findByIdAndIsDeletedFalse(member.getEmployeeId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Employee",
                                    member.getEmployeeId().toString()
                            )
                    );

            if (!employee.isActive()) {
                throw new BusinessException(
                        employee.getFullName() + " is not active",
                        "EMPLOYEE_INACTIVE"
                );
            }

            /*
             * Validate role matches employee's system role
             * (except TEAM_LEAD which is flexible).
             */
            if (member.getRole()
                    != ProjectMemberRole.TEAM_LEAD) {
                validateRoleMatch(employee, member.getRole());
            }

            /*
             * Close existing active assignment for this
             * employee + role combination on this project.
             */
            assignmentRepository
                    .removeCurrentAssignmentForEmployee(
                            projectId,
                            member.getEmployeeId(),
                            ProjectRoleType.valueOf(
                                    member.getRole().name()
                            ),
                            java.time.Instant.now(),
                            assignedBy
                    );

            /*
             * Create new assignment record.
             */
            ProjectAssignment assignment =
                    ProjectAssignment.builder()
                            .projectId(projectId)
                            .employeeId(member.getEmployeeId())
                            .roleInProject(
                                    ProjectRoleType.valueOf(
                                            member.getRole().name()
                                    )
                            )
                            .assignedAt(java.time.Instant.now())
                            .assignedBy(assignedBy)
                            .notes(request.getNotes())
                            .build();

            assignmentRepository.save(assignment);
        }

        /*
         * Auto-activate project when minimum team is present:
         * At least 1 modeler + 1 editor + 1 checker.
         */
        if (project.getStatus() == ProjectStatus.PLANNING
                && hasMinimumTeam(projectId)) {
            project.setStatus(ProjectStatus.ACTIVE);
            project.setActualStartDate(
                    java.time.LocalDate.now()
            );
            log.info(
                    "Project activated: {}",
                    project.getProjectNumber()
            );
        }

        Project saved = projectRepository.save(project);
        return mapToResponse(saved, false);
    }

    private boolean hasMinimumTeam(UUID projectId) {
        var active = assignmentRepository
                .findByProjectIdAndRemovedAtIsNullAndIsDeletedFalse(
                        projectId
                );

        boolean hasModeler = active.stream()
                .anyMatch(a -> a.getRoleInProject()
                        == ProjectRoleType.MODELER);
        boolean hasEditor = active.stream()
                .anyMatch(a -> a.getRoleInProject()
                        == ProjectRoleType.EDITOR);
        boolean hasChecker = active.stream()
                .anyMatch(a -> a.getRoleInProject()
                        == ProjectRoleType.CHECKER);

        return hasModeler && hasEditor && hasChecker;
    }

    private void validateRoleMatch(
            com.thewizecompany.wizevision.employee.domain
                    .Employee employee,
            ProjectMemberRole projectRole) {

        com.thewizecompany.wizevision.employee.domain.Role
                required = switch (projectRole) {
            case MODELER ->
                    com.thewizecompany.wizevision
                            .employee.domain.Role.MODELER;
            case EDITOR ->
                    com.thewizecompany.wizevision
                            .employee.domain.Role.EDITOR;
            case CHECKER ->
                    com.thewizecompany.wizevision
                            .employee.domain.Role.CHECKER;
            case PROJECT_MANAGER ->
                    com.thewizecompany.wizevision
                            .employee.domain.Role.PROJECT_MANAGER;
            default -> null;
        };

        if (required != null
                && employee.getRole() != required) {
            throw new BusinessException(
                    employee.getFullName() +
                            " has role " + employee.getRole().name() +
                            " but you are assigning them as " +
                            projectRole.getDisplayName(),
                    "ROLE_MISMATCH"
            );
        }
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE PROGRESS
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse updateProgress(
            UUID projectId,
            UpdateProjectProgressRequest request,
            UUID updatedBy) {

        Project project = findProject(projectId);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException(
                    "Only ACTIVE projects can be updated",
                    "PROJECT_NOT_ACTIVE"
            );
        }

        if (request.getPhase() != null) {
            project.setCurrentPhase(request.getPhase());
        }

        if (request.getProgressPercentage() != null) {
            project.setProgressPercentage(
                    request.getProgressPercentage()
            );

            /*
             * Auto-set status based on progress.
             */
            if (request.getProgressPercentage() == 100) {
                project.setCurrentPhase(
                        ProjectPhase.COMPLETED
                );
            }
        }

        if (request.getPmNotes() != null) {
            project.setPmNotes(request.getPmNotes());
        }

        Project saved = projectRepository.save(project);

        log.info(
                "Progress updated: {} — {}% — {}",
                project.getProjectNumber(),
                project.getProgressPercentage(),
                project.getCurrentPhase()
        );

        return mapToResponse(saved, false);
    }

    // ─────────────────────────────────────────────────────────
    // STATUS CHANGES
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ProjectResponse putOnHold(
            UUID projectId,
            String reason) {

        Project project = findProject(projectId);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException(
                    "Only ACTIVE projects can be put on hold",
                    "PROJECT_NOT_ACTIVE"
            );
        }

        project.setStatus(ProjectStatus.ON_HOLD);
        project.setOnHoldReason(reason);
        project.setOnHoldAt(Instant.now());

        return mapToResponse(projectRepository.save(project), false);
    }

    @Transactional
    public ProjectResponse resume(UUID projectId) {
        Project project = findProject(projectId);

        if (project.getStatus() != ProjectStatus.ON_HOLD) {
            throw new BusinessException(
                    "Only ON_HOLD projects can be resumed",
                    "PROJECT_NOT_ON_HOLD"
            );
        }

        project.setStatus(ProjectStatus.ACTIVE);
        project.setOnHoldReason(null);
        project.setOnHoldAt(null);

        return mapToResponse(projectRepository.save(project), false);
    }

    @Transactional
    public ProjectResponse markDelivered(UUID projectId) {
        Project project = findProject(projectId);

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new BusinessException(
                    "Project must be ACTIVE to mark as delivered",
                    "PROJECT_NOT_ACTIVE"
            );
        }

        project.setStatus(ProjectStatus.DELIVERED);
        project.setCurrentPhase(ProjectPhase.DELIVERED);
        project.setProgressPercentage(100);

        return mapToResponse(projectRepository.save(project), false);
    }

    @Transactional
    public ProjectResponse complete(UUID projectId) {
        Project project = findProject(projectId);

        if (project.getStatus() != ProjectStatus.DELIVERED) {
            throw new BusinessException(
                    "Project must be DELIVERED to mark as completed",
                    "PROJECT_NOT_DELIVERED"
            );
        }

        project.setStatus(ProjectStatus.COMPLETED);
        project.setCurrentPhase(ProjectPhase.COMPLETED);
        project.setActualEndDate(
                java.time.LocalDate.now()
        );

        return mapToResponse(projectRepository.save(project), false);
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProjectResponse getById(UUID id, boolean showFinancials) {
        return mapToResponse(findProject(id), showFinancials);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> search(
            String search,
            String status,
            UUID clientId,
            UUID pmId,
            Pageable pageable,
            boolean showFinancials) {

        Page<Project> page = projectRepository.searchProjects(
                search, status,
                clientId != null ? clientId.toString() : null,
                pmId != null ? pmId.toString() : null,
                pageable
        );

        return PageResponse.from(
                page.map(p -> mapToResponse(p, showFinancials))
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(UUID employeeId,
                                               boolean showFinancials) {
        return projectRepository
                .findActiveProjectsForEmployee(employeeId)
                .stream()
                .map(p -> mapToResponse(p, showFinancials))
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // CHANGE ORDERS
    // ─────────────────────────────────────────────────────────

    @Transactional
    public ChangeOrderResponse createChangeOrder(
            UUID projectId,
            CreateChangeOrderRequest request,
            UUID pmId) {

        Project project = findProject(projectId);

        if (project.getStatus() == ProjectStatus.COMPLETED
                || project.getStatus()
                == ProjectStatus.CANCELLED) {
            throw new BusinessException(
                    "Cannot add change order to a closed project",
                    "PROJECT_CLOSED"
            );
        }

        /*
         * Generate change order number:
         * {projectNumber}-COR-{sequence}
         * Example: J-TWC-2026-001-COR-001
         */
        long coCount = changeOrderRepository
                .countByProjectIdAndIsDeletedFalse(projectId);

        String changeOrderNumber = project.getProjectNumber()
                + "-COR-"
                + String.format("%03d", coCount + 1);

        ChangeOrder changeOrder = ChangeOrder.builder()
                .changeOrderNumber(changeOrderNumber)
                .projectId(projectId)
                .description(request.getDescription())
                .scopeOfChange(request.getScopeOfChange())
                .notes(request.getNotes())
                .status(ChangeOrderStatus.DRAFT)
                .createdByPmId(pmId)
                .build();

        ChangeOrder saved = changeOrderRepository.save(
                changeOrder
        );

        log.info(
                "Change order created: {}",
                saved.getChangeOrderNumber()
        );

        return mapToCoResponse(saved, project);
    }

    @Transactional
    public ChangeOrderResponse submitChangeOrder(
            UUID projectId,
            UUID changeOrderId) {

        ChangeOrder co = findChangeOrder(changeOrderId);
        Project project = findProject(projectId);

        if (co.getStatus() != ChangeOrderStatus.DRAFT) {
            throw new BusinessException(
                    "Only DRAFT change orders can be submitted",
                    "CO_NOT_DRAFT"
            );
        }

        co.setStatus(ChangeOrderStatus.SUBMITTED);
        co.setSubmittedAt(Instant.now());

        return mapToCoResponse(
                changeOrderRepository.save(co), project
        );
    }

    @Transactional
    public ChangeOrderResponse approveChangeOrder(
            UUID projectId,
            UUID changeOrderId,
            ApproveChangeOrderRequest request,
            UUID adminId) {

        ChangeOrder co = findChangeOrder(changeOrderId);
        Project project = findProject(projectId);

        if (co.getStatus() != ChangeOrderStatus.SUBMITTED) {
            throw new BusinessException(
                    "Change order must be SUBMITTED for approval",
                    "CO_NOT_SUBMITTED"
            );
        }

        co.setAmount(request.getAmount());
        co.setStatus(ChangeOrderStatus.APPROVED);
        co.setApprovedById(adminId);
        co.setApprovedAt(Instant.now());
        if (request.getNotes() != null) {
            co.setNotes(request.getNotes());
        }

        changeOrderRepository.save(co);

        /*
         * Recalculate project's total change order amount.
         */
        var total = changeOrderRepository
                .sumApprovedAmountByProject(projectId);
        project.setChangeOrdersTotal(total);
        projectRepository.save(project);

        log.info(
                "Change order approved: {} — amount: {}",
                co.getChangeOrderNumber(),
                co.getAmount()
        );

        return mapToCoResponse(co, project);
    }

    @Transactional
    public ChangeOrderResponse rejectChangeOrder(
            UUID projectId,
            UUID changeOrderId,
            String reason,
            UUID adminId) {

        ChangeOrder co = findChangeOrder(changeOrderId);
        Project project = findProject(projectId);

        if (co.getStatus() != ChangeOrderStatus.SUBMITTED) {
            throw new BusinessException(
                    "Change order must be SUBMITTED to reject",
                    "CO_NOT_SUBMITTED"
            );
        }

        co.setStatus(ChangeOrderStatus.REJECTED);
        co.setRejectionReason(reason);
        co.setApprovedById(adminId);
        co.setApprovedAt(Instant.now());

        return mapToCoResponse(
                changeOrderRepository.save(co), project
        );
    }

    @Transactional(readOnly = true)
    public List<ChangeOrderResponse> getChangeOrders(
            UUID projectId) {

        Project project = findProject(projectId);

        return changeOrderRepository
                .findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .map(co -> mapToCoResponse(co, project))
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Project findProject(UUID id) {
        return projectRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project", id.toString()
                        )
                );
    }

    private ChangeOrder findChangeOrder(UUID id) {
        return changeOrderRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ChangeOrder", id.toString()
                        )
                );
    }

    private void validateTeamMember(
            UUID employeeId,
            Role requiredRole,
            String roleName) {

        var employee = employeeRepository
                .findByIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                roleName, employeeId.toString()
                        )
                );

        if (!employee.isActive()) {
            throw new BusinessException(
                    roleName + " account is not active",
                    "EMPLOYEE_INACTIVE"
            );
        }

        if (employee.getRole() != requiredRole) {
            throw new BusinessException(
                    "Selected employee is not a " + roleName +
                            ". Their role is: " +
                            employee.getRole().name(),
                    "WRONG_ROLE"
            );
        }
    }

    private void assignRole(
            Project project,
            UUID newEmployeeId,
            ProjectRoleType role,
            UUID previousEmployeeId,
            String assignedBy,
            String notes) {

        /*
         * If same person is being reassigned, skip.
         */
        if (newEmployeeId.equals(previousEmployeeId)) return;

        /*
         * Close the previous assignment if exists.
         */
        if (previousEmployeeId != null) {
            assignmentRepository.removeCurrentAssignment(
                    project.getId(), role,
                    Instant.now(), assignedBy
            );
        }

        /*
         * Create new assignment record.
         */
        ProjectAssignment assignment = ProjectAssignment.builder()
                .projectId(project.getId())
                .employeeId(newEmployeeId)
                .roleInProject(role)
                .assignedAt(Instant.now())
                .assignedBy(assignedBy)
                .notes(notes)
                .build();

        assignmentRepository.save(assignment);
    }

    private String generateProjectNumber() {
        int year = Year.now().getValue();
        Integer sequence = sequenceService.nextSequence(
                SequenceType.PROJECT,
                year
        );
        return String.format("J-TWC-%d-%03d", year, sequence);
    }

    private String getEmployeeName(UUID employeeId) {
        if (employeeId == null) return null;
        return employeeRepository
                .findByIdAndIsDeletedFalse(employeeId)
                .map(Employee::getFullName)
                .orElse(null);
    }

    private ProjectResponse mapToResponse(Project project, boolean showFinancials) {

        var client = clientRepository
                .findByIdAndIsDeletedFalse(project.getClientId())
                .orElse(null);

        String contactName = null;
        if (project.getClientContactId() != null) {
            contactName = contactRepository
                    .findByIdAndIsDeletedFalse(
                            project.getClientContactId()
                    )
                    .map(ClientContact::getFullName)
                    .orElse(null);
        }

        long coCount = changeOrderRepository
                .countByProjectIdAndIsDeletedFalse(
                        project.getId()
                );

        /*
         * Load current active team members from
         * project_assignments table.
         * Only members who have not been removed
         * (removedAt is null).
         */
        List<ProjectResponse.TeamMemberResponse> team =
                assignmentRepository
                        .findByProjectIdAndRemovedAtIsNullAndIsDeletedFalse(
                                project.getId()
                        )
                        .stream()
                        .map(assignment -> {
                            var employee = employeeRepository
                                    .findByIdAndIsDeletedFalse(
                                            assignment.getEmployeeId()
                                    )
                                    .orElse(null);

                            return ProjectResponse.TeamMemberResponse
                                    .builder()
                                    .employeeId(assignment.getEmployeeId())
                                    .employeeCode(employee != null
                                            ? employee.getEmployeeCode() : null)
                                    .fullName(employee != null
                                            ? employee.getFullName() : null)
                                    .role(
                                            assignment.getRoleInProject().name()
                                    )
                                    .roleDisplay(
                                            assignment.getRoleInProject()
                                                    .getDisplayName()
                                    )
                                    .assignedAt(assignment.getAssignedAt())
                                    .assignedBy(assignment.getAssignedBy())
                                    .build();
                        })
                        .toList();

        /*
         * Team is considered assigned when at least one
         * active MODELER, EDITOR, and CHECKER are present.
         */
        boolean teamAssigned = hasMinimumTeam(project.getId());

        return ProjectResponse.builder()
                .id(project.getId())
                .projectNumber(project.getProjectNumber())
                .bidId(project.getBidId())
                .clientId(project.getClientId())
                .clientName(client != null
                        ? client.getCompanyName() : null)
                .clientCode(client != null
                        ? client.getCompanyCode() : null)
                .clientContactId(project.getClientContactId())
                .clientContactName(contactName)
                .projectName(project.getProjectName())
                .projectLocation(project.getProjectLocation())
                .scopeOfWork(project.getScopeOfWork())
                .inclusions(project.getInclusions())
                .exclusions(project.getExclusions())
                .contractAmount(project.getContractAmount())
                .changeOrdersTotal(project.getChangeOrdersTotal())
                .totalContractValue(project.getTotalContractValue())
                .totalInvoiced(project.getTotalInvoiced())
                .totalPaid(project.getTotalPaid())
                .outstandingAmount(project.getOutstandingAmount())
                .status(project.getStatus())
                .statusDisplay(project.getStatus().getDisplayName())
                .currentPhase(project.getCurrentPhase())
                .phaseDisplay(
                        project.getCurrentPhase().getDisplayName()
                )
                .progressPercentage(project.getProgressPercentage())
                .pmId(project.getPmId())
                .pmName(getEmployeeName(project.getPmId()))
                .team(team)                    // ← replaces the 3 fields
                .teamAssigned(teamAssigned)
                .estimatedStartDate(project.getEstimatedStartDate())
                .estimatedEndDate(project.getEstimatedEndDate())
                .actualStartDate(project.getActualStartDate())
                .actualEndDate(project.getActualEndDate())
                .estimatedWeeks(project.getEstimatedWeeks())
                .pmNotes(project.getPmNotes())
                .onHoldReason(project.getOnHoldReason())
                .onHoldAt(project.getOnHoldAt())
                .changeOrdersCount(coCount)
                .createdAt(project.getCreatedAt())
                .createdBy(project.getCreatedBy())
                .build();
    }

    private ChangeOrderResponse mapToCoResponse(
            ChangeOrder co,
            Project project) {

        return ChangeOrderResponse.builder()
                .id(co.getId())
                .changeOrderNumber(co.getChangeOrderNumber())
                .projectId(project.getId())
                .projectNumber(project.getProjectNumber())
                .projectName(project.getProjectName())
                .description(co.getDescription())
                .scopeOfChange(co.getScopeOfChange())
                .status(co.getStatus())
                .statusDisplay(co.getStatus().getDisplayName())
                .amount(co.getAmount())
                .createdByPmId(co.getCreatedByPmId())
                .createdByPmName(
                        getEmployeeName(co.getCreatedByPmId())
                )
                .submittedAt(co.getSubmittedAt())
                .approvedById(co.getApprovedById())
                .approvedByName(
                        getEmployeeName(co.getApprovedById())
                )
                .approvedAt(co.getApprovedAt())
                .rejectionReason(co.getRejectionReason())
                .notes(co.getNotes())
                .createdAt(co.getCreatedAt())
                .build();
    }
}