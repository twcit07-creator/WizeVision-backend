package com.thewizecompany.wizevision.bidding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thewizecompany.wizevision.bidding.domain.Bid;
import com.thewizecompany.wizevision.bidding.domain.BidStatus;
import com.thewizecompany.wizevision.bidding.dto.AdminUpdateBidRequest;
import com.thewizecompany.wizevision.bidding.dto.BidDecisionRequest;
import com.thewizecompany.wizevision.bidding.dto.BidResponseForAdmin;
import com.thewizecompany.wizevision.bidding.dto.BidResponseForPm;
import com.thewizecompany.wizevision.bidding.dto.CreateBidRequest;
import com.thewizecompany.wizevision.bidding.dto.UpdateBidRequest;
import com.thewizecompany.wizevision.bidding.repository.BidRepository;
import com.thewizecompany.wizevision.client.domain.Client;
import com.thewizecompany.wizevision.client.domain.ClientContact;
import com.thewizecompany.wizevision.client.repository.ClientContactRepository;
import com.thewizecompany.wizevision.client.repository.ClientRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.marketing.domain.InquiryStatus;
import com.thewizecompany.wizevision.marketing.domain.ProjectInquiry;
import com.thewizecompany.wizevision.marketing.repository.ProjectInquiryRepository;
import com.thewizecompany.wizevision.projects.service.ProjectService;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
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
public class BidService {

    private final BidRepository bidRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectInquiryRepository inquiryRepository;
    private final ObjectMapper objectMapper;
    private final ProjectService projectService;

    // ─────────────────────────────────────────────────────────
    // CREATE BID (PM)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public BidResponseForPm createBid(
            CreateBidRequest request,
            UUID pmId) {

        /*
         * Validate client exists.
         */
        clientRepository
                .findByIdAndIsDeletedFalse(request.getClientId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Client",
                                request.getClientId().toString()
                        )
                );

        /*
         * If created from inquiry, validate inquiry exists
         * and update its status to BID_IN_PROGRESS.
         */
        if (request.getInquiryId() != null) {
            var inquiry = inquiryRepository
                    .findByIdAndIsDeletedFalse(
                            request.getInquiryId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "ProjectInquiry",
                                    request.getInquiryId().toString()
                            )
                    );

            if (inquiry.getStatus() != InquiryStatus.FORWARDED) {
                throw new BusinessException(
                        "Inquiry is not in FORWARDED status",
                        "INQUIRY_NOT_FORWARDED"
                );
            }

            inquiry.setStatus(InquiryStatus.BID_IN_PROGRESS);
            inquiryRepository.save(inquiry);
        }

        String bidNumber = generateBidNumber();

        Bid bid = Bid.builder()
                .bidNumber(bidNumber)
                .inquiryId(request.getInquiryId())
                .clientId(request.getClientId())
                .clientContactId(request.getClientContactId())
                .projectName(request.getProjectName().trim())
                .projectLocation(request.getProjectLocation())
                .scopeOfWork(request.getScopeOfWork())
                .inclusions(toJson(request.getInclusions()))
                .exclusions(toJson(request.getExclusions()))
                .referenceDocuments(
                        toJson(request.getReferenceDocuments())
                )
                .estimatedWeeks(request.getEstimatedWeeks())
                .proposedStartDate(request.getProposedStartDate())
                .proposedEndDate(request.getProposedEndDate())
                .notes(request.getNotes())
                .status(BidStatus.DRAFT)
                .createdByPmId(pmId)
                .revisionNumber(0)
                .build();

        Bid saved = bidRepository.save(bid);

        log.info(
                "Bid created: {} by PM: {}",
                saved.getBidNumber(), pmId
        );

        return mapToPmResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE BID (PM — only DRAFT)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public BidResponseForPm updateBid(
            UUID bidId,
            UpdateBidRequest request,
            UUID pmId) {

        Bid bid = findBid(bidId);

        validatePmOwnership(bid, pmId);

        if (bid.getStatus() != BidStatus.DRAFT) {
            throw new BusinessException(
                    "Only DRAFT bids can be edited. " +
                            "Current status: " + bid.getStatus().getDisplayName(),
                    "BID_NOT_EDITABLE"
            );
        }

        if (request.getClientContactId() != null)
            bid.setClientContactId(request.getClientContactId());
        if (request.getProjectName() != null)
            bid.setProjectName(request.getProjectName().trim());
        if (request.getProjectLocation() != null)
            bid.setProjectLocation(request.getProjectLocation());
        if (request.getScopeOfWork() != null)
            bid.setScopeOfWork(request.getScopeOfWork());
        if (request.getInclusions() != null)
            bid.setInclusions(toJson(request.getInclusions()));
        if (request.getExclusions() != null)
            bid.setExclusions(toJson(request.getExclusions()));
        if (request.getReferenceDocuments() != null)
            bid.setReferenceDocuments(
                    toJson(request.getReferenceDocuments())
            );
        if (request.getEstimatedWeeks() != null)
            bid.setEstimatedWeeks(request.getEstimatedWeeks());
        if (request.getProposedStartDate() != null)
            bid.setProposedStartDate(request.getProposedStartDate());
        if (request.getProposedEndDate() != null)
            bid.setProposedEndDate(request.getProposedEndDate());
        if (request.getNotes() != null)
            bid.setNotes(request.getNotes());

        return mapToPmResponse(bidRepository.save(bid));
    }

    // ─────────────────────────────────────────────────────────
    // SUBMIT TO ADMIN (PM)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public BidResponseForPm submitToAdmin(
            UUID bidId,
            UUID pmId) {

        Bid bid = findBid(bidId);
        validatePmOwnership(bid, pmId);

        if (bid.getStatus() != BidStatus.DRAFT) {
            throw new BusinessException(
                    "Only DRAFT bids can be submitted",
                    "BID_NOT_DRAFT"
            );
        }

        /*
         * Validate required fields before submission.
         */
        if (bid.getScopeOfWork() == null
                || bid.getScopeOfWork().isBlank()) {
            throw new BusinessException(
                    "Scope of work is required before submission",
                    "SCOPE_REQUIRED"
            );
        }

        if (bid.getEstimatedWeeks() == null) {
            throw new BusinessException(
                    "Estimated timeline is required before submission",
                    "TIMELINE_REQUIRED"
            );
        }

        bid.setStatus(BidStatus.SUBMITTED);
        bid.setSubmittedAt(Instant.now());

        Bid saved = bidRepository.save(bid);

        log.info("Bid submitted to admin: {}", bid.getBidNumber());

        /*
         * TODO: Notify admin via notification module.
         */

        return mapToPmResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN — SET AMOUNT AND REVIEW
    // ─────────────────────────────────────────────────────────

    @Transactional
    public BidResponseForAdmin adminUpdateBid(
            UUID bidId,
            AdminUpdateBidRequest request,
            UUID adminId) {

        Bid bid = findBid(bidId);

        if (bid.getStatus() != BidStatus.SUBMITTED
                && bid.getStatus() != BidStatus.NEGOTIATING) {
            throw new BusinessException(
                    "Bid must be SUBMITTED or NEGOTIATING " +
                            "for admin review",
                    "INVALID_BID_STATUS"
            );
        }

        bid.setBidAmount(request.getBidAmount());
        if (request.getInternalNotes() != null) {
            bid.setInternalNotes(request.getInternalNotes());
        }
        bid.setReviewedByAdminId(adminId);
        bid.setStatus(BidStatus.UNDER_REVIEW);

        Bid saved = bidRepository.save(bid);

        log.info(
                "Admin updated bid: {} — amount set",
                bid.getBidNumber()
        );

        return mapToAdminResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN — SEND TO CLIENT
    // ─────────────────────────────────────────────────────────

    @Transactional
    public BidResponseForAdmin sendToClient(
            UUID bidId,
            UUID adminId) {

        Bid bid = findBid(bidId);

        if (bid.getStatus() != BidStatus.UNDER_REVIEW) {
            throw new BusinessException(
                    "Bid must be UNDER_REVIEW before sending to client",
                    "INVALID_BID_STATUS"
            );
        }

        if (bid.getBidAmount() == null) {
            throw new BusinessException(
                    "Bid amount must be set before sending to client",
                    "AMOUNT_REQUIRED"
            );
        }

        bid.setStatus(BidStatus.SENT_TO_CLIENT);
        bid.setSentToClientAt(Instant.now());
        bid.setReviewedByAdminId(adminId);

        Bid saved = bidRepository.save(bid);

        log.info(
                "Bid sent to client: {}",
                bid.getBidNumber()
        );

        /*
         * TODO: Send email to client contact
         * via notification module.
         */

        return mapToAdminResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN — RECORD CLIENT DECISION
    // ─────────────────────────────────────────────────────────

    @Transactional
    public BidResponseForAdmin recordDecision(
            UUID bidId,
            BidDecisionRequest request,
            UUID adminId) {

        Bid bid = findBid(bidId);

        if (bid.getStatus() != BidStatus.SENT_TO_CLIENT
                && bid.getStatus() != BidStatus.NEGOTIATING) {
            throw new BusinessException(
                    "Bid must be SENT_TO_CLIENT or NEGOTIATING " +
                            "to record a decision",
                    "INVALID_BID_STATUS"
            );
        }

        bid.setDecidedAt(Instant.now());
        bid.setReviewedByAdminId(adminId);

        switch (request.getDecision()) {

            case ACCEPTED -> {
                bid.setStatus(BidStatus.ACCEPTED);

                if (bid.getInquiryId() != null) {
                    inquiryRepository
                            .findByIdAndIsDeletedFalse(bid.getInquiryId())
                            .ifPresent(inquiry -> {
                                inquiry.setStatus(InquiryStatus.BID_CREATED);
                                inquiry.setBidId(bid.getId());
                                inquiryRepository.save(inquiry);
                            });
                }

                // Save bid first so project can reference it
                bidRepository.save(bid);

                /*
                 * Auto-create project from accepted bid.
                 * Project number: J-TWC-2026-001
                 */
                projectService.createFromBid(bid.getId(), adminId);

                log.info("Bid ACCEPTED: {} → Project created",
                        bid.getBidNumber());

                // Return early — bid already saved above
                return mapToAdminResponse(
                        bidRepository.findByIdAndIsDeletedFalse(bid.getId())
                                .orElse(bid)
                );
            }

            case REJECTED -> {
                if (request.getReason() == null
                        || request.getReason().isBlank()) {
                    throw new BusinessException(
                            "Rejection reason is required",
                            "REASON_REQUIRED"
                    );
                }
                bid.setStatus(BidStatus.REJECTED);
                bid.setRejectionReason(request.getReason());

                log.info(
                        "Bid REJECTED: {} — reason: {}",
                        bid.getBidNumber(),
                        request.getReason()
                );
            }

            case NEGOTIATING -> {
                bid.setStatus(BidStatus.NEGOTIATING);
                bid.setRevisionNumber(
                        bid.getRevisionNumber() + 1
                );

                log.info(
                        "Bid NEGOTIATING: {} — revision: {}",
                        bid.getBidNumber(),
                        bid.getRevisionNumber()
                );
            }
        }

        return mapToAdminResponse(bidRepository.save(bid));
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BidResponseForPm getBidForPm(UUID bidId, UUID pmId) {
        Bid bid = findBid(bidId);
        validatePmOwnership(bid, pmId);
        return mapToPmResponse(bid);
    }

    @Transactional(readOnly = true)
    public BidResponseForAdmin getBidForAdmin(UUID bidId) {
        return mapToAdminResponse(findBid(bidId));
    }

    @Transactional(readOnly = true)
    public PageResponse<BidResponseForPm> searchBidsForPm(
            String search,
            String status,
            UUID pmId,
            Pageable pageable) {

        Page<Bid> page = bidRepository.searchBids(
                search, status, null,
                pmId != null ? pmId.toString() : null,
                pageable
        );

        return PageResponse.from(
                page.map(this::mapToPmResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<BidResponseForAdmin> searchBidsForAdmin(
            String search,
            String status,
            UUID clientId,
            Pageable pageable) {

        Page<Bid> page = bidRepository.searchBids(
                search, status,
                clientId != null ? clientId.toString() : null,
                null,
                pageable
        );

        return PageResponse.from(
                page.map(this::mapToAdminResponse)
        );
    }

    /*
     * Bids waiting for admin action.
     * Used in admin dashboard notification badge.
     */
    @Transactional(readOnly = true)
    public List<BidResponseForAdmin> getPendingAdminReview() {
        return bidRepository
                .findByStatusAndIsDeletedFalse(BidStatus.SUBMITTED)
                .stream()
                .map(this::mapToAdminResponse)
                .toList();
    }

    /*
     * PM's active bids dashboard.
     */
    @Transactional(readOnly = true)
    public List<BidResponseForPm> getMyActiveBids(UUID pmId) {
        return bidRepository
                .findByCreatedByPmIdAndStatusAndIsDeletedFalse(
                        pmId, BidStatus.DRAFT
                )
                .stream()
                .map(this::mapToPmResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Bid findBid(UUID id) {
        return bidRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Bid", id.toString()
                        )
                );
    }

    private void validatePmOwnership(Bid bid, UUID pmId) {
        if (!bid.getCreatedByPmId().equals(pmId)) {
            throw new BusinessException(
                    "You do not have access to this bid",
                    "BID_ACCESS_DENIED"
            );
        }
    }

    private String generateBidNumber() {
        int year = Year.now().getValue();
        long count = bidRepository.countByIsDeletedFalse();
        return "BID-" + year + "-"
                + String.format("%03d", count + 1);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn(
                    "Could not serialize to JSON: {}",
                    e.getMessage()
            );
            return null;
        }
    }

    private BidResponseForPm mapToPmResponse(Bid bid) {
        String clientName = clientRepository
                .findByIdAndIsDeletedFalse(bid.getClientId())
                .map(Client::getCompanyName)
                .orElse(null);

        String clientCode = clientRepository
                .findByIdAndIsDeletedFalse(bid.getClientId())
                .map(Client::getCompanyCode)
                .orElse(null);

        String contactName = null;
        if (bid.getClientContactId() != null) {
            contactName = contactRepository
                    .findByIdAndIsDeletedFalse(
                            bid.getClientContactId()
                    )
                    .map(ClientContact::getFullName)
                    .orElse(null);
        }

        String inquiryNumber = null;
        if (bid.getInquiryId() != null) {
            inquiryNumber = inquiryRepository
                    .findByIdAndIsDeletedFalse(bid.getInquiryId())
                    .map(ProjectInquiry::getInquiryNumber)
                    .orElse(null);
        }

        return BidResponseForPm.builder()
                .id(bid.getId())
                .bidNumber(bid.getBidNumber())
                .revisionNumber(bid.getRevisionNumber())
                .inquiryId(bid.getInquiryId())
                .inquiryNumber(inquiryNumber)
                .clientId(bid.getClientId())
                .clientName(clientName)
                .clientCode(clientCode)
                .clientContactId(bid.getClientContactId())
                .clientContactName(contactName)
                .projectName(bid.getProjectName())
                .projectLocation(bid.getProjectLocation())
                .scopeOfWork(bid.getScopeOfWork())
                .inclusions(bid.getInclusions())
                .exclusions(bid.getExclusions())
                .referenceDocuments(bid.getReferenceDocuments())
                .estimatedWeeks(bid.getEstimatedWeeks())
                .proposedStartDate(bid.getProposedStartDate())
                .proposedEndDate(bid.getProposedEndDate())
                .status(bid.getStatus())
                .statusDisplay(bid.getStatus().getDisplayName())
                .notes(bid.getNotes())
                .submittedAt(bid.getSubmittedAt())
                .sentToClientAt(bid.getSentToClientAt())
                .decidedAt(bid.getDecidedAt())
                .rejectionReason(bid.getRejectionReason())
                .convertedProjectId(bid.getConvertedProjectId())
                .createdAt(bid.getCreatedAt())
                .build();
    }

    private BidResponseForAdmin mapToAdminResponse(Bid bid) {
        var client = clientRepository
                .findByIdAndIsDeletedFalse(bid.getClientId())
                .orElse(null);

        String contactName = null;
        String contactEmail = null;
        String contactPhone = null;
        if (bid.getClientContactId() != null) {
            var contact = contactRepository
                    .findByIdAndIsDeletedFalse(
                            bid.getClientContactId()
                    )
                    .orElse(null);
            if (contact != null) {
                contactName = contact.getFullName();
                contactEmail = contact.getEmail();
                contactPhone = contact.getPhone();
            }
        }

        String pmName = employeeRepository
                .findByIdAndIsDeletedFalse(bid.getCreatedByPmId())
                .map(Employee::getFullName)
                .orElse(null);

        String inquiryNumber = null;
        if (bid.getInquiryId() != null) {
            inquiryNumber = inquiryRepository
                    .findByIdAndIsDeletedFalse(bid.getInquiryId())
                    .map(ProjectInquiry::getInquiryNumber)
                    .orElse(null);
        }

        return BidResponseForAdmin.builder()
                .id(bid.getId())
                .bidNumber(bid.getBidNumber())
                .revisionNumber(bid.getRevisionNumber())
                .inquiryId(bid.getInquiryId())
                .inquiryNumber(inquiryNumber)
                .clientId(bid.getClientId())
                .clientName(client != null
                        ? client.getCompanyName() : null)
                .clientCode(client != null
                        ? client.getCompanyCode() : null)
                .clientEmail(client != null
                        ? client.getEmail() : null)
                .clientPhone(client != null
                        ? client.getPhone() : null)
                .clientContactId(bid.getClientContactId())
                .clientContactName(contactName)
                .clientContactEmail(contactEmail)
                .clientContactPhone(contactPhone)
                .projectName(bid.getProjectName())
                .projectLocation(bid.getProjectLocation())
                .scopeOfWork(bid.getScopeOfWork())
                .inclusions(bid.getInclusions())
                .exclusions(bid.getExclusions())
                .referenceDocuments(bid.getReferenceDocuments())
                .estimatedWeeks(bid.getEstimatedWeeks())
                .proposedStartDate(bid.getProposedStartDate())
                .proposedEndDate(bid.getProposedEndDate())
                .bidAmount(bid.getBidAmount())
                .internalNotes(bid.getInternalNotes())
                .status(bid.getStatus())
                .statusDisplay(bid.getStatus().getDisplayName())
                .createdByPmId(bid.getCreatedByPmId())
                .createdByPmName(pmName)
                .notes(bid.getNotes())
                .submittedAt(bid.getSubmittedAt())
                .sentToClientAt(bid.getSentToClientAt())
                .decidedAt(bid.getDecidedAt())
                .rejectionReason(bid.getRejectionReason())
                .convertedProjectId(bid.getConvertedProjectId())
                .createdAt(bid.getCreatedAt())
                .createdBy(bid.getCreatedBy())
                .build();
    }
}