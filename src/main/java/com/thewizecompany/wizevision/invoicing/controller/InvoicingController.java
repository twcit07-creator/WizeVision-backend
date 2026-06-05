package com.thewizecompany.wizevision.invoicing.controller;

import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.invoicing.dto.CreateInvoiceRequest;
import com.thewizecompany.wizevision.invoicing.dto.InvoiceResponse;
import com.thewizecompany.wizevision.invoicing.dto.InvoiceSummaryResponse;
import com.thewizecompany.wizevision.invoicing.dto.PaymentResponse;
import com.thewizecompany.wizevision.invoicing.dto.RecordPaymentRequest;
import com.thewizecompany.wizevision.invoicing.service.InvoicingService;
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
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(
        name = "Invoicing",
        description = "Invoice creation and payment tracking"
)
@SecurityRequirement(name = "bearerAuth")
public class InvoicingController {

    private final InvoicingService invoicingService;
    private final EmployeeRepository employeeRepository;

    // ─────────────────────────────────────────────────────────
    // INVOICES
    // ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @Operation(
            summary = "Create a new invoice",
            description = "Creates DRAFT invoice for a project. " +
                    "Line items define what is being billed."
    )
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(
            @Valid @RequestBody CreateInvoiceRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        invoicingService.createInvoice(
                                request, resolveEmployeeId(email)
                        ),
                        "Invoice created"
                ));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(summary = "Get invoice by ID with payments")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(invoicingService.getById(id))
        );
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')"
    )
    @Operation(summary = "Search invoices")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceSummaryResponse>>>
    search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        invoicingService.search(
                                search, status, clientId, projectId,
                                PageRequest.of(
                                        page, size,
                                        Sort.by("invoiceDate").descending()
                                )
                        )
                )
        );
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(summary = "Get all invoices for a project")
    public ResponseEntity<ApiResponse<List<InvoiceSummaryResponse>>>
    getByProject(@PathVariable UUID projectId) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        invoicingService.getByProject(projectId)
                )
        );
    }

    @PatchMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @Operation(
            summary = "Mark invoice as sent to client",
            description = "Future: will trigger email automatically."
    )
    public ResponseEntity<ApiResponse<InvoiceResponse>> send(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        invoicingService.markAsSent(id),
                        "Invoice marked as sent"
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    @Operation(
            summary = "Cancel an invoice",
            description = "Cannot cancel if payments exist or " +
                    "if fully paid."
    )
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        invoicingService.cancelInvoice(id, email),
                        "Invoice cancelled"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // PAYMENTS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE')")
    @Operation(
            summary = "Record payment against invoice",
            description = "Supports partial payments. " +
                    "Invoice auto-moves to PAID when fully paid."
    )
    public ResponseEntity<ApiResponse<PaymentResponse>>
    recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request,
            @AuthenticationPrincipal String email) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        invoicingService.recordPayment(
                                id, request, resolveEmployeeId(email)
                        ),
                        "Payment recorded successfully"
                ));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(summary = "Get all payments for an invoice")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>>
    getPayments(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        invoicingService.getPaymentsForInvoice(id)
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