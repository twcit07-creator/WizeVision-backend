package com.thewizecompany.wizevision.template.service;

import com.thewizecompany.wizevision.hr.repository.PayslipRepository;
import com.thewizecompany.wizevision.invoicing.repository.InvoiceRepository;
import com.thewizecompany.wizevision.invoicing.repository.PaymentRepository;
import com.thewizecompany.wizevision.bidding.repository.BidRepository;
import com.thewizecompany.wizevision.client.repository.ClientContactRepository;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.projects.repository.ProjectRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.template.domain.TemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/*
 * PDF GENERATION SERVICE
 *
 * Takes a template + data → renders HTML → converts to PDF.
 *
 * FLOW:
 * 1. Load template from database
 * 2. Build Thymeleaf context with document data
 * 3. Render HTML string using Thymeleaf engine
 * 4. Convert rendered HTML to PDF using Flying Saucer
 * 5. Return PDF as byte array
 *
 * Thymeleaf is used as a string template engine here
 * (not MVC view rendering).
 * Templates are loaded from the database, not from files.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final TemplateService templateService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final BidRepository bidRepository;
    private final PayslipRepository payslipRepository;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final EmployeeRepository employeeRepository;
    private final SpringTemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─────────────────────────────────────────────────────────
    // INVOICE PDF
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(
            UUID invoiceId,
            UUID templateId) {

        var invoice = invoiceRepository
                .findByIdAndIsDeletedFalse(invoiceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice", invoiceId.toString()
                        )
                );

        var template = templateId != null
                ? templateService.getTemplateById(templateId)
                : templateService.getDefaultTemplate(
                TemplateType.INVOICE
        );

        /*
         * Build data context.
         * All variables accessible in template as ${variable}
         */
        Context ctx = new Context();

        /*
         * Invoice fields.
         */
        ctx.setVariable("invoiceNumber",
                invoice.getInvoiceNumber());
        ctx.setVariable("invoiceDate",
                invoice.getInvoiceDate().format(DATE_FORMAT));
        ctx.setVariable("dueDate",
                invoice.getDueDate() != null
                        ? invoice.getDueDate().format(DATE_FORMAT)
                        : "N/A");
        ctx.setVariable("status",
                invoice.getStatus().getDisplayName());
        ctx.setVariable("subtotal",
                invoice.getSubtotal());
        ctx.setVariable("taxPercentage",
                invoice.getTaxPercentage());
        ctx.setVariable("taxAmount",
                invoice.getTaxAmount());
        ctx.setVariable("totalAmount",
                invoice.getTotalAmount());
        ctx.setVariable("amountPaid",
                invoice.getAmountPaid());
        ctx.setVariable("outstandingAmount",
                invoice.getOutstandingAmount());
        ctx.setVariable("notes", invoice.getNotes());
        ctx.setVariable("termsAndConditions",
                invoice.getTermsAndConditions());
        ctx.setVariable("lineItems",
                invoice.getLineItems());

        /*
         * Client fields.
         */
        var client = clientRepository
                .findByIdAndIsDeletedFalse(invoice.getClientId())
                .orElse(null);

        ctx.setVariable("clientName",
                client != null ? client.getCompanyName() : "");
        ctx.setVariable("clientCode",
                client != null ? client.getCompanyCode() : "");
        ctx.setVariable("clientGst",
                client != null && client.getGstNumber() != null
                        ? client.getGstNumber() : "");
        ctx.setVariable("clientAddress",
                buildAddress(client));

        /*
         * Contact fields.
         */
        if (invoice.getClientContactId() != null) {
            var contact = contactRepository
                    .findByIdAndIsDeletedFalse(
                            invoice.getClientContactId()
                    )
                    .orElse(null);
            ctx.setVariable("contactName",
                    contact != null ? contact.getFullName() : "");
            ctx.setVariable("contactEmail",
                    contact != null
                            ? contact.getEmail() : "");
            ctx.setVariable("contactPhone",
                    contact != null
                            ? contact.getPhone() : "");
        }

        /*
         * Project fields.
         */
        var project = projectRepository
                .findByIdAndIsDeletedFalse(
                        invoice.getProjectId()
                )
                .orElse(null);
        ctx.setVariable("projectNumber",
                project != null ? project.getProjectNumber() : "");
        ctx.setVariable("projectName",
                project != null ? project.getProjectName() : "");

        /*
         * Payments.
         */
        var payments = paymentRepository
                .findByInvoiceIdAndIsDeletedFalseOrderByPaymentDateDesc(
                        invoiceId
                );
        ctx.setVariable("payments", payments);

        /*
         * Company info (WizeVision).
         */
        ctx.setVariable("companyName", "The WizeVision Company");
        ctx.setVariable("companyAddress",
                "Your Office Address, City, State - Pincode");
        ctx.setVariable("companyGst", "Your GST Number");
        ctx.setVariable("companyEmail",
                "billing@wizevision.com");
        ctx.setVariable("companyPhone", "+91 XXXXXXXXXX");

        return renderToPdf(
                template.getContent(), ctx
        );
    }

    // ─────────────────────────────────────────────────────────
    // BID PDF
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateBidPdf(
            UUID bidId,
            UUID templateId) {

        var bid = bidRepository
                .findByIdAndIsDeletedFalse(bidId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bid", bidId.toString()
                        )
                );

        var template = templateId != null
                ? templateService.getTemplateById(templateId)
                : templateService.getDefaultTemplate(
                TemplateType.BID
        );

        Context ctx = new Context();

        ctx.setVariable("bidNumber", bid.getBidNumber());
        ctx.setVariable("revisionNumber",
                bid.getRevisionNumber());
        ctx.setVariable("projectName", bid.getProjectName());
        ctx.setVariable("projectLocation",
                bid.getProjectLocation());
        ctx.setVariable("scopeOfWork", bid.getScopeOfWork());
        ctx.setVariable("inclusions", bid.getInclusions());
        ctx.setVariable("exclusions", bid.getExclusions());
        ctx.setVariable("referenceDocuments",
                bid.getReferenceDocuments());
        ctx.setVariable("estimatedWeeks",
                bid.getEstimatedWeeks());
        ctx.setVariable("proposedStartDate",
                bid.getProposedStartDate() != null
                        ? bid.getProposedStartDate()
                          .format(DATE_FORMAT) : "TBD");
        ctx.setVariable("proposedEndDate",
                bid.getProposedEndDate() != null
                        ? bid.getProposedEndDate()
                          .format(DATE_FORMAT) : "TBD");
        ctx.setVariable("status",
                bid.getStatus().getDisplayName());
        ctx.setVariable("notes", bid.getNotes());

        var client = clientRepository
                .findByIdAndIsDeletedFalse(bid.getClientId())
                .orElse(null);
        ctx.setVariable("clientName",
                client != null ? client.getCompanyName() : "");
        ctx.setVariable("clientAddress",
                buildAddress(client));

        if (bid.getClientContactId() != null) {
            var contact = contactRepository
                    .findByIdAndIsDeletedFalse(
                            bid.getClientContactId()
                    )
                    .orElse(null);
            ctx.setVariable("contactName",
                    contact != null
                            ? contact.getFullName() : "");
            ctx.setVariable("contactDesignation",
                    contact != null
                            ? contact.getDesignation() : "");
        }

        ctx.setVariable("companyName",
                "The WizeVision Company");
        ctx.setVariable("companyAddress",
                "Your Office Address, City, State - Pincode");
        ctx.setVariable("companyEmail",
                "info@wizevision.com");
        ctx.setVariable("companyPhone", "+91 XXXXXXXXXX");

        return renderToPdf(template.getContent(), ctx);
    }

    // ─────────────────────────────────────────────────────────
    // PAYSLIP PDF
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generatePayslipPdf(
            UUID payslipId,
            UUID templateId) {

        var payslip = payslipRepository
                .findByIdAndIsDeletedFalse(payslipId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payslip", payslipId.toString()
                        )
                );

        var template = templateId != null
                ? templateService.getTemplateById(templateId)
                : templateService.getDefaultTemplate(
                TemplateType.PAYSLIP
        );

        Context ctx = new Context();

        var employee = employeeRepository
                .findByIdAndIsDeletedFalse(
                        payslip.getEmployeeId()
                )
                .orElse(null);

        String[] monthNames = {
                "January","February","March","April",
                "May","June","July","August",
                "September","October","November","December"
        };

        ctx.setVariable("employeeCode",
                employee != null
                        ? employee.getEmployeeCode() : "");
        ctx.setVariable("employeeName",
                employee != null ? employee.getFullName() : "");
        ctx.setVariable("department",
                employee != null
                        && employee.getDepartment() != null
                        ? employee.getDepartment().getName() : "");
        ctx.setVariable("designation",
                employee != null
                        && employee.getDesignation() != null
                        ? employee.getDesignation().getTitle() : "");
        ctx.setVariable("panNumber",
                employee != null
                        && employee.getPanNumber() != null
                        ? employee.getPanNumber() : "");
        ctx.setVariable("pfNumber",
                employee != null
                        && employee.getPfNumber() != null
                        ? employee.getPfNumber() : "");

        ctx.setVariable("month",
                monthNames[payslip.getMonth() - 1]);
        ctx.setVariable("year", payslip.getYear());
        ctx.setVariable("totalWorkingDays",
                payslip.getTotalWorkingDays());
        ctx.setVariable("presentDays",
                payslip.getPresentDays());
        ctx.setVariable("paidLeaveDays",
                payslip.getPaidLeaveDays());
        ctx.setVariable("unpaidLeaveDays",
                payslip.getUnpaidLeaveDays());

        ctx.setVariable("earnings", payslip.getEarnings());
        ctx.setVariable("deductions",
                payslip.getDeductions());
        ctx.setVariable("grossEarnings",
                payslip.getGrossEarnings());
        ctx.setVariable("totalDeductions",
                payslip.getTotalDeductions());
        ctx.setVariable("netPay", payslip.getNetPay());

        ctx.setVariable("pfEmployee",
                payslip.getPfEmployee());
        ctx.setVariable("pfEmployer",
                payslip.getPfEmployer());
        ctx.setVariable("esiEmployee",
                payslip.getEsiEmployee());
        ctx.setVariable("esiEmployer",
                payslip.getEsiEmployer());
        ctx.setVariable("professionalTax",
                payslip.getProfessionalTax());

        ctx.setVariable("companyName",
                "The WizeVision Company");
        ctx.setVariable("companyAddress",
                "Your Office Address, City, State - Pincode");

        return renderToPdf(template.getContent(), ctx);
    }

    // ─────────────────────────────────────────────────────────
    // CORE RENDERER
    // ─────────────────────────────────────────────────────────

    private byte[] renderToPdf(
            String templateContent,
            Context context) {

        try {
            /*
             * STEP 1: Render HTML using Thymeleaf.
             * Template content is processed as a string,
             * not loaded from a file.
             * Thymeleaf replaces ${variable} expressions
             * with actual values from the context.
             */
            String renderedHtml = templateEngine
                    .process(templateContent, context);

            /*
             * STEP 2: Convert rendered HTML to PDF
             * using Flying Saucer (XHTML renderer).
             */
            try (ByteArrayOutputStream outputStream =
                         new ByteArrayOutputStream()) {

                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(renderedHtml);
                renderer.layout();
                renderer.createPDF(outputStream);

                return outputStream.toByteArray();
            }

        } catch (Exception e) {
            log.error(
                    "PDF generation failed: {}",
                    e.getMessage(), e
            );
            throw new BusinessException(
                    "Failed to generate PDF: " + e.getMessage(),
                    "PDF_GENERATION_FAILED"
            );
        }
    }

    private String buildAddress(
            com.thewizecompany.wizevision
                    .client.domain.Client client) {

        if (client == null) return "";

        StringBuilder sb = new StringBuilder();
        if (client.getAddressLine1() != null)
            sb.append(client.getAddressLine1());
        if (client.getAddressLine2() != null)
            sb.append(", ").append(client.getAddressLine2());
        if (client.getCity() != null)
            sb.append(", ").append(client.getCity());
        if (client.getState() != null)
            sb.append(", ").append(client.getState());
        if (client.getPincode() != null)
            sb.append(" - ").append(client.getPincode());

        return sb.toString();
    }
}