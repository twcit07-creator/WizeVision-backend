package com.thewizecompany.wizevision.marketing.controller;

import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.marketing.dto.ConvertLeadRequest;
import com.thewizecompany.wizevision.marketing.dto.CreateInquiryRequest;
import com.thewizecompany.wizevision.marketing.dto.CreateLeadRequest;
import com.thewizecompany.wizevision.marketing.dto.ForwardInquiryRequest;
import com.thewizecompany.wizevision.marketing.dto.InquiryResponse;
import com.thewizecompany.wizevision.marketing.dto.LeadResponse;
import com.thewizecompany.wizevision.marketing.dto.UpdateLeadRequest;
import com.thewizecompany.wizevision.marketing.service.MarketingService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketing")
@RequiredArgsConstructor
@Tag(
        name = "Marketing",
        description = "Lead management and project inquiry forwarding"
)
@SecurityRequirement(name = "bearerAuth")
public class MarketingController {

    private final MarketingService marketingService;
    private final EmployeeRepository employeeRepository;

    // ─────────────────────────────────────────────────────────
    // LEADS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/leads")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
    )
    @Operation(summary = "Create a new lead")
    public ResponseEntity<ApiResponse<LeadResponse>> createLead(
            @Valid @RequestBody CreateLeadRequest request,
            @AuthenticationPrincipal String currentUserEmail) {

        UUID currentUserId = resolveEmployeeId(currentUserEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        marketingService.createLead(
                                request, currentUserId
                        ),
                        "Lead created successfully"
                ));
    }

    @GetMapping("/leads/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(summary = "Get lead by ID")
    public ResponseEntity<ApiResponse<LeadResponse>> getLead(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.getLeadById(id)
                )
        );
    }

    @GetMapping("/leads")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(
            summary = "Search leads",
            description = "Filter by status and assigned marketing exec"
    )
    public ResponseEntity<ApiResponse<PageResponse<LeadResponse>>>
    searchLeads(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID assignedToId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.searchLeads(
                                search,
                                status,
                                assignedToId,
                                PageRequest.of(
                                        page, size

                                )
                        )
                )
        );
    }

    @GetMapping("/leads/my")
    @PreAuthorize(
            "hasAnyRole('MARKETING_EXECUTIVE','SUPER_ADMIN','ADMIN')"
    )
    @Operation(summary = "Get my assigned leads")
    public ResponseEntity<ApiResponse<PageResponse<LeadResponse>>>
    getMyLeads(
            @AuthenticationPrincipal String currentUserEmail,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID currentUserId = resolveEmployeeId(currentUserEmail);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.searchLeads(
                                null,
                                status,
                                currentUserId,
                                PageRequest.of(
                                        page, size,
                                        Sort.by("createdAt").descending()
                                )
                        )
                )
        );
    }

    @PutMapping("/leads/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
    )
    @Operation(summary = "Update lead details")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLead(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLeadRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.updateLead(id, request),
                        "Lead updated successfully"
                )
        );
    }

//    @PostMapping("/leads/{id}/convert")
//    @PreAuthorize(
//            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
//    )
//    @Operation(
//            summary = "Convert lead to client + create inquiry",
//            description = "Marks lead as converted, creates a client " +
//                    "(or links existing one), and creates a " +
//                    "project inquiry ready to forward to PM."
//    )
//    public ResponseEntity<ApiResponse<InquiryResponse>>
//    convertLead(
//            @PathVariable UUID id,
//            @Valid @RequestBody ConvertLeadRequest request) {
//
//        return ResponseEntity.ok(
//                ApiResponse.ok(
//                        marketingService.convertLead(id, request),
//                        "Lead converted successfully. " +
//                                "Inquiry created — forward to PM when ready."
//                )
//        );
//    }

    // ─────────────────────────────────────────────────────────
    // PROJECT INQUIRIES
    // ─────────────────────────────────────────────────────────

//    @PostMapping("/inquiries")
//    @PreAuthorize(
//            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
//    )
//    @Operation(
//            summary = "Create direct inquiry (existing client)",
//            description = "Use when an existing client calls " +
//                    "directly with a new project."
//    )
//    public ResponseEntity<ApiResponse<InquiryResponse>>
//    createDirectInquiry(
//            @Valid @RequestBody CreateInquiryRequest request) {
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(ApiResponse.ok(
//                        marketingService.createDirectInquiry(request),
//                        "Inquiry created successfully"
//                ));
//    }
@PostMapping("/inquiries")
@PreAuthorize(
        "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
)
@Operation(
        summary = "Create a project inquiry",
        description = """
        Creates a project inquiry from one of two sources:

        FROM A LEAD (prospect company):
          Provide leadId only. clientId must be null.
          Lead status is automatically updated to CONVERTED.
          The company becomes a client only when the bid is accepted.

        FROM AN EXISTING CLIENT (direct inquiry):
          Provide clientId only. leadId must be null.
          Use when an existing client calls with a new project.

        Exactly one of leadId or clientId must be provided.
        """
)
public ResponseEntity<ApiResponse<InquiryResponse>>
createInquiry(
        @Valid @RequestBody CreateInquiryRequest request,
        @AuthenticationPrincipal String currentUserEmail) {

    UUID currentUserId = resolveEmployeeId(currentUserEmail);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(
                    marketingService.createInquiry(
                            request, currentUserId
                    ),
                    "Inquiry created successfully"
            ));
}

    @GetMapping("/inquiries/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(summary = "Get inquiry by ID")
    public ResponseEntity<ApiResponse<InquiryResponse>>
    getInquiry(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.getInquiryById(id)
                )
        );
    }

    @GetMapping("/inquiries")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(summary = "Search project inquiries")
    public ResponseEntity<ApiResponse<PageResponse<InquiryResponse>>>
    searchInquiries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID forwardedToId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.searchInquiries(
                                search,
                                status,
                                forwardedToId,
                                PageRequest.of(
                                        page, size
                                )
                        )
                )
        );
    }

    @GetMapping("/inquiries/my")
    @PreAuthorize("hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "PM inbox — inquiries forwarded to me",
            description = "Returns all FORWARDED inquiries " +
                    "waiting for the PM to create a bid."
    )
    public ResponseEntity<ApiResponse<List<InquiryResponse>>>
    getMyInquiries(
            @AuthenticationPrincipal String currentUserEmail) {

        UUID currentUserId = resolveEmployeeId(currentUserEmail);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.getMyInquiries(currentUserId)
                )
        );
    }

    @PostMapping("/inquiries/{id}/forward")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','MARKETING_EXECUTIVE')"
    )
    @Operation(
            summary = "Forward inquiry to Project Manager",
            description = "Marketing selects a PM and forwards " +
                    "the inquiry for bidding."
    )
    public ResponseEntity<ApiResponse<InquiryResponse>>
    forwardToPm(
            @PathVariable UUID id,
            @Valid @RequestBody ForwardInquiryRequest request,
            @AuthenticationPrincipal String currentUserEmail) {

        UUID currentUserId = resolveEmployeeId(currentUserEmail);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        marketingService.forwardToPm(
                                id, request, currentUserId
                        ),
                        "Inquiry forwarded to Project Manager"
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