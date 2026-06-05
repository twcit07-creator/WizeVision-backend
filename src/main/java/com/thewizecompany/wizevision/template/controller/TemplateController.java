package com.thewizecompany.wizevision.template.controller;

import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.template.domain.TemplateType;
import com.thewizecompany.wizevision.template.dto.CreateTemplateRequest;
import com.thewizecompany.wizevision.template.dto.TemplateResponse;
import com.thewizecompany.wizevision.template.dto.UpdateTemplateRequest;
import com.thewizecompany.wizevision.template.service.PdfGenerationService;
import com.thewizecompany.wizevision.template.service.TemplateService;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(
        name = "Templates",
        description = "Document template management and PDF generation"
)
@SecurityRequirement(name = "bearerAuth")
public class TemplateController {

    private final TemplateService templateService;
    private final PdfGenerationService pdfGenerationService;
    private final EmployeeRepository employeeRepository;

    // ─────────────────────────────────────────────────────────
    // TEMPLATE MANAGEMENT (IT Admin)
    // ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')"
    )
    @Operation(
            summary = "Upload a new document template",
            description = "Upload an HTML/Thymeleaf template. " +
                    "Use ${variable} syntax for dynamic data."
    )
    public ResponseEntity<ApiResponse<TemplateResponse>> create(
            @Valid @RequestBody CreateTemplateRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        templateService.createTemplate(request),
                        "Template created successfully"
                ));
    }

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')"
    )
    @Operation(summary = "Get all templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>>
    getAll(
            @RequestParam(required = false)
            TemplateType type) {

        List<TemplateResponse> templates = type != null
                ? templateService.getByType(type)
                : templateService.getAll();

        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')"
    )
    @Operation(
            summary = "Get template by ID (includes content)"
    )
    public ResponseEntity<ApiResponse<TemplateResponse>>
    getById(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(templateService.getById(id))
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')"
    )
    @Operation(summary = "Update a template")
    public ResponseEntity<ApiResponse<TemplateResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        templateService.updateTemplate(id, request),
                        "Template updated"
                )
        );
    }

    @PatchMapping("/{id}/set-default")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')"
    )
    @Operation(
            summary = "Set template as default for its type",
            description = "Previous default is automatically demoted."
    )
    public ResponseEntity<ApiResponse<TemplateResponse>>
    setDefault(@PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        templateService.setAsDefault(id),
                        "Template set as default"
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','IT_ADMIN')"
    )
    @Operation(
            summary = "Delete a template",
            description = "Cannot delete the default template."
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal String email) {

        templateService.deleteTemplate(id, email);
        return ResponseEntity.ok(
                ApiResponse.ok("Template deleted")
        );
    }

    // ─────────────────────────────────────────────────────────
    // PDF GENERATION
    // ─────────────────────────────────────────────────────────

    @GetMapping("/pdf/invoice/{invoiceId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','FINANCE'," +
                    "'PROJECT_MANAGER')"
    )
    @Operation(
            summary = "Generate invoice PDF",
            description = "Returns PDF file for download. " +
                    "Uses default template if templateId " +
                    "is not specified."
    )
    public ResponseEntity<byte[]> generateInvoicePdf(
            @PathVariable UUID invoiceId,
            @RequestParam(required = false) UUID templateId) {

        byte[] pdf = pdfGenerationService
                .generateInvoicePdf(invoiceId, templateId);

        return buildPdfResponse(
                pdf, "INV-" + invoiceId + ".pdf"
        );
    }

    @GetMapping("/pdf/bid/{bidId}")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','PROJECT_MANAGER')"
    )
    @Operation(
            summary = "Generate bid PDF",
            description = "Returns bid document as PDF."
    )
    public ResponseEntity<byte[]> generateBidPdf(
            @PathVariable UUID bidId,
            @RequestParam(required = false) UUID templateId) {

        byte[] pdf = pdfGenerationService
                .generateBidPdf(bidId, templateId);

        return buildPdfResponse(
                pdf, "BID-" + bidId + ".pdf"
        );
    }

    @GetMapping("/pdf/payslip/{payslipId}")
    @Operation(
            summary = "Generate payslip PDF",
            description = "Employee can download their own payslip. " +
                    "HR and Admin can download any payslip."
    )
    public ResponseEntity<byte[]> generatePayslipPdf(
            @PathVariable UUID payslipId,
            @RequestParam(required = false) UUID templateId) {

        byte[] pdf = pdfGenerationService
                .generatePayslipPdf(payslipId, templateId);

        return buildPdfResponse(
                pdf, "PAYSLIP-" + payslipId + ".pdf"
        );
    }

    // ─────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> buildPdfResponse(
            byte[] pdf,
            String filename) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename)
                        .build()
        );
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}