package com.thewizecompany.wizevision.bidding.controller;

import com.thewizecompany.wizevision.bidding.dto.AdminUpdateBidRequest;
import com.thewizecompany.wizevision.bidding.dto.BidDecisionRequest;
import com.thewizecompany.wizevision.bidding.dto.BidResponseForAdmin;
import com.thewizecompany.wizevision.bidding.dto.BidResponseForPm;
import com.thewizecompany.wizevision.bidding.dto.CreateBidRequest;
import com.thewizecompany.wizevision.bidding.dto.UpdateBidRequest;
import com.thewizecompany.wizevision.bidding.service.BidService;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
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

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
@Tag(
        name = "Bidding",
        description = "Bid creation, review, and client decision workflow"
)
@SecurityRequirement(name = "bearerAuth")
public class BidController {

    private final BidService bidService;
    private final EmployeeRepository employeeRepository;

    // ─────────────────────────────────────────────────────────
    // PM ENDPOINTS
    // ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')"
    )
    @Operation(
            summary = "PM — Create a new bid",
            description = "Creates a DRAFT bid. Amount is not set here."
    )
    public ResponseEntity<ApiResponse<BidResponseForPm>> create(
            @Valid @RequestBody CreateBidRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        bidService.createBid(
                                request, resolveEmployeeId(email)
                        ),
                        "Bid created as DRAFT"
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')"
    )
    @Operation(summary = "PM — Update DRAFT bid")
    public ResponseEntity<ApiResponse<BidResponseForPm>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBidRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.updateBid(
                                id, request, resolveEmployeeId(email)
                        ),
                        "Bid updated"
                )
        );
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize(
            "hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')"
    )
    @Operation(
            summary = "PM — Submit bid to admin",
            description = "Moves bid from DRAFT to SUBMITTED. " +
                    "Scope of work and timeline must be filled."
    )
    public ResponseEntity<ApiResponse<BidResponseForPm>> submit(
            @PathVariable UUID id,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.submitToAdmin(
                                id, resolveEmployeeId(email)
                        ),
                        "Bid submitted to admin for review"
                )
        );
    }

    @GetMapping("/my")
    @PreAuthorize(
            "hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')"
    )
    @Operation(summary = "PM — Get my bids")
    public ResponseEntity<ApiResponse<PageResponse<BidResponseForPm>>>
    getMyBids(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.searchBidsForPm(
                                search, status,
                                resolveEmployeeId(email),
                                PageRequest.of(
                                        page, size,
                                        Sort.by("createdAt").descending()
                                )
                        )
                )
        );
    }

    @GetMapping("/{id}/pm")
    @PreAuthorize(
            "hasAnyRole('PROJECT_MANAGER','SUPER_ADMIN','ADMIN')"
    )
    @Operation(
            summary = "PM — Get bid by ID (amount hidden)",
            description = "Returns bid without bid amount field."
    )
    public ResponseEntity<ApiResponse<BidResponseForPm>>
    getBidForPm(
            @PathVariable UUID id,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.getBidForPm(
                                id, resolveEmployeeId(email)
                        )
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN ENDPOINTS
    // ─────────────────────────────────────────────────────────

    @GetMapping("/pending-review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Bids waiting for review",
            description = "All SUBMITTED bids waiting for admin " +
                    "to set amount."
    )
    public ResponseEntity<ApiResponse<List<BidResponseForAdmin>>>
    getPendingReview() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.getPendingAdminReview()
                )
        );
    }

    @GetMapping("/{id}/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Get bid by ID (full details)",
            description = "Returns bid with bid amount and " +
                    "internal notes visible."
    )
    public ResponseEntity<ApiResponse<BidResponseForAdmin>>
    getBidForAdmin(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(bidService.getBidForAdmin(id))
        );
    }

    @PatchMapping("/{id}/amount")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Set bid amount",
            description = "Admin sets the final bid amount. " +
                    "Moves to UNDER_REVIEW status."
    )
    public ResponseEntity<ApiResponse<BidResponseForAdmin>>
    setAmount(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateBidRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.adminUpdateBid(
                                id, request, resolveEmployeeId(email)
                        ),
                        "Bid amount set successfully"
                )
        );
    }

    @PatchMapping("/{id}/send-to-client")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Mark bid as sent to client",
            description = "Records that bid was sent to client. " +
                    "Future: will trigger email automatically."
    )
    public ResponseEntity<ApiResponse<BidResponseForAdmin>>
    sendToClient(
            @PathVariable UUID id,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.sendToClient(
                                id, resolveEmployeeId(email)
                        ),
                        "Bid marked as sent to client"
                )
        );
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Admin — Record client decision",
            description = "Record ACCEPTED, REJECTED, or NEGOTIATING. " +
                    "ACCEPTED triggers project creation (next module)."
    )
    public ResponseEntity<ApiResponse<BidResponseForAdmin>>
    recordDecision(
            @PathVariable UUID id,
            @Valid @RequestBody BidDecisionRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.recordDecision(
                                id, request, resolveEmployeeId(email)
                        ),
                        "Decision recorded"
                )
        );
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Admin — Search all bids")
    public ResponseEntity<ApiResponse<PageResponse<BidResponseForAdmin>>>
    getAllBids(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        bidService.searchBidsForAdmin(
                                search, status, clientId,
                                PageRequest.of(
                                        page, size,
                                        Sort.by("createdAt").descending()
                                )
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