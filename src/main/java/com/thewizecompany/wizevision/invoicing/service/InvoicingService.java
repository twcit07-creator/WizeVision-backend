package com.thewizecompany.wizevision.invoicing.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.thewizecompany.wizevision.client.domain.Client;
import com.thewizecompany.wizevision.client.repository.ClientContactRepository;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.invoicing.domain.*;
import com.thewizecompany.wizevision.invoicing.dto.CreateInvoiceRequest;
import com.thewizecompany.wizevision.invoicing.dto.InvoiceResponse;
import com.thewizecompany.wizevision.invoicing.dto.InvoiceSummaryResponse;
import com.thewizecompany.wizevision.invoicing.dto.PaymentResponse;
import com.thewizecompany.wizevision.invoicing.dto.RecordPaymentRequest;
import com.thewizecompany.wizevision.invoicing.repository.InvoiceRepository;
import com.thewizecompany.wizevision.invoicing.repository.PaymentRepository;
import com.thewizecompany.wizevision.projects.repository.ProjectRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.thewizecompany.wizevision.invoicing.domain.BillingStatus;
import com.thewizecompany.wizevision.invoicing.domain.InvoiceTargetType;
import com.thewizecompany.wizevision.projects.repository.ChangeOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final ChangeOrderRepository changeOrderRepository;

    // ─────────────────────────────────────────────────────────
    // CREATE INVOICE
    // ─────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────
// CREATE INVOICE — BillNova pattern with strict
//                  over-billing protection
// ─────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse createInvoice(
            CreateInvoiceRequest request,
            UUID createdById) {

        var project = projectRepository
                .findByIdAndIsDeletedFalse(request.getProjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project",
                                request.getProjectId().toString()
                        )
                );

        /*
         * STEP 1: Resolve the change order if applicable.
         */
        com.thewizecompany.wizevision.projects.domain
                .ChangeOrder changeOrder = null;

        if (request.getTargetType()
                == InvoiceTargetType.CHANGE_ORDER) {

            if (request.getChangeOrderId() == null) {
                throw new BusinessException(
                        "Change order ID is required when " +
                                "target type is CHANGE_ORDER",
                        "CHANGE_ORDER_REQUIRED"
                );
            }

            changeOrder = changeOrderRepository
                    .findByIdAndIsDeletedFalse(
                            request.getChangeOrderId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "ChangeOrder",
                                    request.getChangeOrderId().toString()
                            )
                    );

            if (!changeOrder.getProjectId()
                    .equals(project.getId())) {
                throw new BusinessException(
                        "Change order does not belong " +
                                "to this project",
                        "CHANGE_ORDER_PROJECT_MISMATCH"
                );
            }

            if (changeOrder.getStatus()
                    != com.thewizecompany.wizevision
                    .projects.domain
                    .ChangeOrderStatus.APPROVED) {
                throw new BusinessException(
                        "Only APPROVED change orders can be invoiced. " +
                                "Current status: " +
                                changeOrder.getStatus().getDisplayName(),
                        "CHANGE_ORDER_NOT_APPROVED"
                );
            }
        }

        /*
         * STEP 2: Calculate the invoice amount.
         * If percentage is provided and > 0, use it.
         * Otherwise, use the raw amount.
         */
        BigDecimal targetAmount = resolveTargetAmount(
                project, changeOrder, request.getTargetType()
        );

        BigDecimal finalAmount;
        BigDecimal billingPercentage = null;

        if (request.getPercentage() != null
                && request.getPercentage()
                .compareTo(BigDecimal.ZERO) > 0) {

            finalAmount = targetAmount
                    .multiply(request.getPercentage())
                    .divide(
                            new BigDecimal("100"),
                            2,
                            java.math.RoundingMode.HALF_UP
                    );
            billingPercentage = request.getPercentage();

        } else if (request.getAmount() != null
                && request.getAmount()
                .compareTo(BigDecimal.ZERO) > 0) {

            finalAmount = request.getAmount();

        } else {
            throw new BusinessException(
                    "You must provide a valid amount or " +
                            "percentage greater than zero",
                    "INVALID_AMOUNT"
            );
        }

        /*
         * STEP 3: OVER-BILLING CHECK
         *
         * This is the critical protection.
         * Sum all existing invoices for this exact target
         * (same project + same target type + same change order).
         * New total must not exceed the target amount.
         */
        BigDecimal alreadyInvoiced = getAlreadyInvoicedAmount(
                project.getId(),
                request.getTargetType(),
                changeOrder != null ? changeOrder.getId() : null
        );

        BigDecimal newTotal = alreadyInvoiced.add(finalAmount);

        if (newTotal.compareTo(targetAmount) > 0) {
            BigDecimal remaining =
                    targetAmount.subtract(alreadyInvoiced);
            throw new BusinessException(
                    String.format(
                            "Over-billing denied. " +
                                    "You cannot invoice %.2f. " +
                                    "Remaining balance on %s is %.2f",
                            finalAmount,
                            request.getTargetType()
                                    == InvoiceTargetType.CONTRACT_BASE
                                    ? "base contract"
                                    : "change order " +
                                    Objects.requireNonNull(changeOrder).getChangeOrderNumber(),
                            remaining
                    ),
                    "OVER_BILLING"
            );
        }

        /*
         * STEP 4: Determine billing status.
         * FULL_AND_FINAL if new total = target amount exactly.
         * PARTIAL if new total < target amount.
         */
        BillingStatus billingStatus =
                newTotal.compareTo(targetAmount) == 0
                        ? BillingStatus.FULL_AND_FINAL
                        : BillingStatus.PARTIAL;

        /*
         * STEP 5: Calculate tax.
         */
        BigDecimal taxPct = request.getTaxPercentage() != null
                ? request.getTaxPercentage()
                : BigDecimal.ZERO;

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (taxPct.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = finalAmount
                    .multiply(taxPct)
                    .divide(
                            new BigDecimal("100"),
                            2,
                            java.math.RoundingMode.HALF_UP
                    );
        }

        BigDecimal totalAmount = finalAmount.add(taxAmount);

        String invoiceNumber = generateInvoiceNumber();

        String lineItemsJson = null;
        if (request.getLineItems() != null
                && !request.getLineItems().isEmpty()) {
            try {
                lineItemsJson = objectMapper.writeValueAsString(
                        request.getLineItems()
                );
            } catch (com.fasterxml.jackson.core
                             .JsonProcessingException e) {
                log.warn("Could not serialize line items");
            }
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .projectId(project.getId())
                .clientId(project.getClientId())
                .clientContactId(
                        request.getClientContactId() != null
                                ? request.getClientContactId()
                                : project.getClientContactId()
                )
                .invoiceDate(request.getInvoiceDate())
                .dueDate(request.getDueDate())
                .subtotal(finalAmount)
                .taxPercentage(taxPct)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .targetType(request.getTargetType())
                .changeOrderId(
                        changeOrder != null
                                ? changeOrder.getId() : null
                )
                .billingStatus(billingStatus)
                .billingPercentage(billingPercentage)
                .lineItems(lineItemsJson)
                .notes(request.getNotes())
                .termsAndConditions(
                        request.getTermsAndConditions()
                )
                .status(InvoiceStatus.DRAFT)
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        /*
         * STEP 6: Update project totals.
         */
        BigDecimal newTotalInvoiced = invoiceRepository
                .findByProjectIdAndIsDeletedFalse(project.getId())
                .stream()
                .filter(i ->
                        i.getStatus() != InvoiceStatus.CANCELLED
                )
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        project.setTotalInvoiced(newTotalInvoiced);
        projectRepository.save(project);

        log.info(
                "Invoice created: {} for project: {} " +
                        "target: {} billing: {}",
                saved.getInvoiceNumber(),
                project.getProjectNumber(),
                request.getTargetType(),
                billingStatus
        );

        return mapToResponse(saved);
    }

    /*
     * Returns the target amount for billing.
     * CONTRACT_BASE → project contract amount
     * CHANGE_ORDER  → change order amount
     */
    private BigDecimal resolveTargetAmount(
            com.thewizecompany.wizevision.projects.domain
                    .Project project,
            com.thewizecompany.wizevision.projects.domain
                    .ChangeOrder changeOrder,
            InvoiceTargetType targetType) {

        if (targetType == InvoiceTargetType.CONTRACT_BASE) {
            if (project.getContractAmount() == null
                    || project.getContractAmount()
                    .compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                        "Project has no contract amount set",
                        "NO_CONTRACT_AMOUNT"
                );
            }
            return project.getContractAmount();
        }

        if (changeOrder == null
                || changeOrder.getAmount() == null
                || changeOrder.getAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "Change order has no amount set",
                    "NO_CHANGE_ORDER_AMOUNT"
            );
        }
        return changeOrder.getAmount();
    }

    /*
     * Sum of all non-canceled invoices for this exact target.
     * CONTRACT_BASE: all CONTRACT_BASE invoices for this project.
     * CHANGE_ORDER:  all CHANGE_ORDER invoices for this
     *                specific change order.
     */
    private BigDecimal getAlreadyInvoicedAmount(
            UUID projectId,
            InvoiceTargetType targetType,
            UUID changeOrderId) {

        return invoiceRepository
                .findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .filter(i ->
                        i.getStatus() != InvoiceStatus.CANCELLED
                )
                .filter(i ->
                        i.getTargetType() == targetType
                )
                .filter(i -> {
                    if (targetType
                            == InvoiceTargetType.CHANGE_ORDER) {
                        return changeOrderId != null
                                && changeOrderId.equals(
                                i.getChangeOrderId()
                        );
                    }
                    return true;
                })
                .map(Invoice::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─────────────────────────────────────────────────────────
    // MARK AS SENT
    // ─────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse markAsSent(UUID invoiceId) {
        Invoice invoice = findInvoice(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException(
                    "Only DRAFT invoices can be marked as sent",
                    "INVOICE_NOT_DRAFT"
            );
        }

        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(Instant.now());

        return mapToResponse(invoiceRepository.save(invoice));
    }

    // ─────────────────────────────────────────────────────────
    // RECORD PAYMENT
    // ─────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse recordPayment(
            UUID invoiceId,
            RecordPaymentRequest request,
            UUID recordedById) {

        Invoice invoice = findInvoice(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException(
                    "Cannot record payment for a cancelled invoice",
                    "INVOICE_CANCELLED"
            );
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException(
                    "Invoice is already fully paid",
                    "INVOICE_ALREADY_PAID"
            );
        }

        /*
         * Validate payment does not exceed outstanding.
         */
        BigDecimal outstanding = invoice.getOutstandingAmount();
        if (request.getAmount()
                .compareTo(outstanding) > 0) {
            throw new BusinessException(
                    "Payment amount (" + request.getAmount() +
                            ") exceeds outstanding balance (" +
                            outstanding + ")",
                    "PAYMENT_EXCEEDS_OUTSTANDING"
            );
        }

        String paymentNumber = generatePaymentNumber();

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .invoiceId(invoiceId)
                .projectId(invoice.getProjectId())
                .clientId(invoice.getClientId())
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMode(request.getPaymentMode())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .recordedById(recordedById)
                .build();

        paymentRepository.save(payment);

        /*
         * Update invoice paid amount.
         */
        BigDecimal newAmountPaid = invoice.getAmountPaid()
                .add(request.getAmount());
        invoice.setAmountPaid(newAmountPaid);

        /*
         * Update invoice status.
         */
        if (invoice.isFullyPaid()) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(Instant.now());
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);

        /*
         * Update project's total paid amount.
         */
        var totalPaid = paymentRepository
                .sumByProjectId(invoice.getProjectId());

        projectRepository
                .findByIdAndIsDeletedFalse(
                        invoice.getProjectId()
                )
                .ifPresent(project -> {
                    project.setTotalPaid(totalPaid);

                    /*
                     * Update total invoiced on project.
                     */
                    BigDecimal totalInvoiced =
                            invoiceRepository
                                    .findByProjectIdAndIsDeletedFalse(
                                            invoice.getProjectId()
                                    )
                                    .stream()
                                    .filter(inv ->
                                            inv.getStatus()
                                                    != InvoiceStatus.CANCELLED
                                    )
                                    .map(Invoice::getTotalAmount)
                                    .reduce(BigDecimal.ZERO,
                                            BigDecimal::add);

                    project.setTotalInvoiced(totalInvoiced);
                    projectRepository.save(project);
                });

        log.info(
                "Payment recorded: {} — {} for invoice: {}",
                paymentNumber,
                request.getAmount(),
                invoice.getInvoiceNumber()
        );

        return mapPaymentToResponse(payment, invoice);
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        return mapToResponse(findInvoice(id));
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryResponse> getByProject(
            UUID projectId) {
        return invoiceRepository
                .findByProjectIdAndIsDeletedFalse(projectId)
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryResponse> search(
            String search,
            String status,
            UUID clientId,
            UUID projectId,
            Pageable pageable) {

        Page<Invoice> page = invoiceRepository.searchInvoices(
                search, status,
                clientId != null ? clientId.toString() : null,
                projectId != null ? projectId.toString() : null,
                pageable
        );

        return PageResponse.from(
                page.map(this::mapToSummary)
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForInvoice(
            UUID invoiceId) {

        Invoice invoice = findInvoice(invoiceId);

        return paymentRepository
                .findByInvoiceIdAndIsDeletedFalseOrderByPaymentDateDesc(
                        invoiceId
                )
                .stream()
                .map(p -> mapPaymentToResponse(p, invoice))
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // CANCEL INVOICE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse cancelInvoice(
            UUID invoiceId,
            String cancelledBy) {

        Invoice invoice = findInvoice(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException(
                    "Cannot cancel a fully paid invoice",
                    "INVOICE_PAID"
            );
        }

        if (invoice.getAmountPaid()
                .compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(
                    "Cannot cancel an invoice with payments. " +
                            "Reverse the payments first.",
                    "INVOICE_HAS_PAYMENTS"
            );
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.markAsDeleted(cancelledBy);

        return mapToResponse(invoiceRepository.save(invoice));
    }

    // ─────────────────────────────────────────────────────────
    // SCHEDULED — MARK OVERDUE
    // ─────────────────────────────────────────────────────────

    /*
     * Runs every day at midnight.
     * Marks sent/partially-paid invoices as OVERDUE
     * if their due date has passed.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueInvoices() {
        List<Invoice> overdue =
                invoiceRepository.findOverdueInvoices(
                        LocalDate.now()
                );

        overdue.forEach(invoice -> {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        });

        if (!overdue.isEmpty()) {
            log.info(
                    "Marked {} invoices as overdue",
                    overdue.size()
            );
        }
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Invoice findInvoice(UUID id) {
        return invoiceRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Invoice", id.toString()
                        )
                );
    }

    private String generateInvoiceNumber() {
        int year = Year.now().getValue();
        long count = invoiceRepository.countByIsDeletedFalse();
        return "INV-" + year + "-"
                + String.format("%03d", count + 1);
    }

    private String generatePaymentNumber() {
        int year = Year.now().getValue();
        long count = paymentRepository.countByIsDeletedFalse();
        return "PAY-" + year + "-"
                + String.format("%03d", count + 1);
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        var client = clientRepository
                .findByIdAndIsDeletedFalse(invoice.getClientId())
                .orElse(null);

        String contactName = null;
        String contactEmail = null;
        if (invoice.getClientContactId() != null) {
            var contact = contactRepository
                    .findByIdAndIsDeletedFalse(
                            invoice.getClientContactId()
                    )
                    .orElse(null);
            if (contact != null) {
                contactName = contact.getFullName();
                contactEmail = contact.getEmail();
            }
        }

        String projectNumber = null;
        String projectName = null;
        var project = projectRepository
                .findByIdAndIsDeletedFalse(
                        invoice.getProjectId()
                )
                .orElse(null);
        if (project != null) {
            projectNumber = project.getProjectNumber();
            projectName = project.getProjectName();
        }

        var payments = paymentRepository
                .findByInvoiceIdAndIsDeletedFalseOrderByPaymentDateDesc(
                        invoice.getId()
                )
                .stream()
                .map(p -> mapPaymentToResponse(p, invoice))
                .toList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .projectId(invoice.getProjectId())
                .projectNumber(projectNumber)
                .projectName(projectName)
                .clientId(invoice.getClientId())
                .clientName(client != null
                        ? client.getCompanyName() : null)
                .clientCode(client != null
                        ? client.getCompanyCode() : null)
                .clientGstNumber(client != null
                        ? client.getGstNumber() : null)
                .clientContactId(invoice.getClientContactId())
                .clientContactName(contactName)
                .clientContactEmail(contactEmail)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .taxPercentage(invoice.getTaxPercentage())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .amountPaid(invoice.getAmountPaid())
                .outstandingAmount(invoice.getOutstandingAmount())
                .status(invoice.getStatus())
                .statusDisplay(
                        invoice.getStatus().getDisplayName()
                )
                .overdue(invoice.isOverdue())
                .lineItems(invoice.getLineItems())
                .notes(invoice.getNotes())
                .termsAndConditions(
                        invoice.getTermsAndConditions()
                )
                .sentAt(invoice.getSentAt())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .createdBy(invoice.getCreatedBy())
                .payments(payments)
                .build();
    }

    private InvoiceSummaryResponse mapToSummary(
            Invoice invoice) {

        String projectNumber = null;
        String projectName = null;
        var project = projectRepository
                .findByIdAndIsDeletedFalse(invoice.getProjectId())
                .orElse(null);
        if (project != null) {
            projectNumber = project.getProjectNumber();
            projectName = project.getProjectName();
        }

        String clientName = clientRepository
                .findByIdAndIsDeletedFalse(invoice.getClientId())
                .map(Client::getCompanyName)
                .orElse(null);

        return InvoiceSummaryResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .projectId(invoice.getProjectId())
                .projectNumber(projectNumber)
                .projectName(projectName)
                .clientId(invoice.getClientId())
                .clientName(clientName)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .totalAmount(invoice.getTotalAmount())
                .amountPaid(invoice.getAmountPaid())
                .outstandingAmount(invoice.getOutstandingAmount())
                .status(invoice.getStatus())
                .statusDisplay(invoice.getStatus().getDisplayName())
                .overdue(invoice.isOverdue())
                .build();
    }

    private PaymentResponse mapPaymentToResponse(
            Payment payment,
            Invoice invoice) {

        String projectNumber = null;
        var project = projectRepository
                .findByIdAndIsDeletedFalse(payment.getProjectId())
                .orElse(null);
        if (project != null) {
            projectNumber = project.getProjectNumber();
        }

        String recordedByName = null;
        if (payment.getRecordedById() != null) {
            recordedByName = employeeRepository
                    .findByIdAndIsDeletedFalse(
                            payment.getRecordedById()
                    )
                    .map(Employee::getFullName)
                    .orElse(null);
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .invoiceId(payment.getInvoiceId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .projectId(payment.getProjectId())
                .projectNumber(projectNumber)
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMode(payment.getPaymentMode())
                .paymentModeDisplay(
                        payment.getPaymentMode().getDisplayName()
                )
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .recordedById(payment.getRecordedById())
                .recordedByName(recordedByName)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}