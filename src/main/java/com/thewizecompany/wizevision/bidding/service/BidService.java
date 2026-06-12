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
import com.thewizecompany.wizevision.marketing.domain.Lead;
import com.thewizecompany.wizevision.marketing.domain.ProjectInquiry;
import com.thewizecompany.wizevision.marketing.repository.LeadRepository;
import com.thewizecompany.wizevision.marketing.repository.ProjectInquiryRepository;
import com.thewizecompany.wizevision.projects.service.ProjectService;
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
public class BidService {

    private final BidRepository bidRepository;
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectInquiryRepository inquiryRepository;
    private final ObjectMapper objectMapper;
    private final ProjectService projectService;
    private final LeadRepository leadRepository;
    private final SequenceService sequenceService;

    // ─────────────────────────────────────────────────────────
    // CREATE BID (PM)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public BidResponseForPm createBid(
            CreateBidRequest request,
            UUID pmId) {

        /*
         * VALIDATION:
         * Must have inquiryId OR clientId.
         * If no inquiry and no client → reject.
         */
        if (request.getInquiryId() == null
                && request.getClientId() == null) {
            throw new BusinessException(
                    "A bid must be linked to either a project " +
                            "inquiry or an existing client.",
                    "BID_SOURCE_REQUIRED"
            );
        }

        UUID resolvedClientId = null;
        String resolvedCompanyName = null;
        UUID resolvedContactId = request.getClientContactId();

        // ── PATH A: FROM INQUIRY ──────────────────────────────
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
                        "Inquiry must be in FORWARDED status. " +
                                "Current status: " +
                                inquiry.getStatus().getDisplayName(),
                        "INQUIRY_NOT_FORWARDED"
                );
            }

            /*
             * Resolve company info from inquiry.
             *
             * Case 1: Inquiry has clientId (direct inquiry)
             *   → Use existing client
             *
             * Case 2: Inquiry has leadId (lead-based inquiry)
             *   → Company is still a prospect
             *   → clientId stays null on bid
             *   → Company name comes from lead
             */
            if (inquiry.getClientId() != null) {
                /*
                 * Direct inquiry — client already exists.
                 */
                resolvedClientId = inquiry.getClientId();

                resolvedCompanyName = clientRepository
                        .findByIdAndIsDeletedFalse(
                                inquiry.getClientId()
                        )
                        .map(Client::getCompanyName)
                        .orElse(null);

            } else if (inquiry.getLeadId() != null) {
                /*
                 * Lead-based inquiry — prospect not yet a client.
                 * clientId remains null.
                 * Company name comes from the lead record.
                 */
                resolvedCompanyName = leadRepository
                        .findByIdAndIsDeletedFalse(
                                inquiry.getLeadId()
                        )
                        .map(Lead::getCompanyName)
                        .orElse("Unknown Company");
            }

            /*
             * Use contact from inquiry if PM did not specify one.
             */
            if (resolvedContactId == null) {
                resolvedContactId = inquiry.getClientContactId();
            }

            /*
             * Update inquiry status.
             */
            inquiry.setStatus(InquiryStatus.BID_IN_PROGRESS);
            inquiryRepository.save(inquiry);
        }

        // ── PATH B: DIRECT BID (no inquiry) ──────────────────
        if (request.getInquiryId() == null
                && request.getClientId() != null) {

            var client = clientRepository
                    .findByIdAndIsDeletedFalse(
                            request.getClientId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Client",
                                    request.getClientId().toString()
                            )
                    );

            resolvedClientId = client.getId();
            resolvedCompanyName = client.getCompanyName();
        }

        String bidNumber = generateBidNumber();

        Bid bid = Bid.builder()
                .bidNumber(bidNumber)
                .inquiryId(request.getInquiryId())
                .clientId(resolvedClientId)   // null for prospect bids
                .clientContactId(resolvedContactId)
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
                "Bid created: {} for company: {} by PM: {}",
                saved.getBidNumber(),
                resolvedCompanyName,
                pmId
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
                bidRepository.save(bid);

                /*
                 * If this bid came from a lead-based inquiry,
                 * NOW is when we create the client record.
                 * The prospect has accepted our bid — they are
                 * officially a client.
                 */
                if (bid.getInquiryId() != null) {
                    inquiryRepository
                            .findByIdAndIsDeletedFalse(bid.getInquiryId())
                            .ifPresent(inquiry -> {
                                inquiry.setStatus(InquiryStatus.BID_CREATED);
                                inquiry.setBidId(bid.getId());

                                /*
                                 * If inquiry came from a lead and has no
                                 * client yet, create the client now.
                                 */
                                if (inquiry.getLeadId() != null
                                        && inquiry.getClientId() == null) {

                                    leadRepository
                                            .findByIdAndIsDeletedFalse(
                                                    inquiry.getLeadId()
                                            )
                                            .ifPresent(lead -> {
                                                /*
                                                 * Create client from lead data.
                                                 * This is the moment a prospect
                                                 * becomes a client.
                                                 */
                                                Client newClient = Client.builder()
                                                        .companyCode(
                                                                generateClientCode()
                                                        )
                                                        .companyName(
                                                                lead.getCompanyName()
                                                        )
                                                        .email(lead.getContactEmail())
                                                        .phone(lead.getContactPhone())
                                                        .city(lead.getCity())
                                                        .state(lead.getState())
                                                        .country(lead.getCountry())
                                                        .industryType(
                                                                lead.getIndustryType()
                                                        )
                                                        .isActive(true)
                                                        .build();

                                                Client savedClient =
                                                        clientRepository.save(newClient);

                                                /*
                                                 * Link the client back to the
                                                 * inquiry and the lead.
                                                 */
                                                inquiry.setClientId(
                                                        savedClient.getId()
                                                );
                                                lead.setClientId(
                                                        savedClient.getId()
                                                );
                                                leadRepository.save(lead);

                                                /*
                                                 * Also update the bid's clientId
                                                 * so the project gets the right client.
                                                 */
                                                bid.setClientId(savedClient.getId());

                                                log.info(
                                                        "Client created from accepted bid: " +
                                                                "{} → {}",
                                                        lead.getCompanyName(),
                                                        savedClient.getCompanyCode()
                                                );
                                            });
                                }

                                inquiryRepository.save(inquiry);
                            });
                }

                projectService.createFromBid(bid.getId(), adminId);

                log.info(
                        "Bid ACCEPTED: {} → Project created",
                        bid.getBidNumber()
                );

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
        Integer sequence = sequenceService.nextSequence(
                SequenceType.BID,
                year
        );
        return String.format("TWC-BID-%d-%03d", year, sequence);
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

        /*
         * Resolve company name:
         * 1. If clientId exists → load from client table
         * 2. If no clientId but has inquiryId → load from
         *    inquiry → lead → company name
         * 3. Fallback → null
         */
        String companyName = resolveCompanyName(bid);
        String companyCode = null;

        if (bid.getClientId() != null) {
            var client = clientRepository
                    .findByIdAndIsDeletedFalse(bid.getClientId())
                    .orElse(null);
            if (client != null) {
                companyCode = client.getCompanyCode();
            }
        }

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
                .companyName(companyName)
                .clientCode(companyCode)
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

    /*
     * Resolves the company name regardless of whether
     * a client record exists yet.
     */
    private String resolveCompanyName(Bid bid) {

        /*
         * Case 1: Client record exists → use it.
         */
        if (bid.getClientId() != null) {
            return clientRepository
                    .findByIdAndIsDeletedFalse(bid.getClientId())
                    .map(Client::getCompanyName)
                    .orElse(null);
        }

        /*
         * Case 2: No client yet but has inquiry → check lead.
         */
        if (bid.getInquiryId() != null) {
            return inquiryRepository
                    .findByIdAndIsDeletedFalse(bid.getInquiryId())
                    .flatMap(inquiry -> {
                        if (inquiry.getLeadId() != null) {
                            return leadRepository
                                    .findByIdAndIsDeletedFalse(
                                            inquiry.getLeadId()
                                    )
                                    .map(Lead::getCompanyName);
                        }
                        return java.util.Optional.empty();
                    })
                    .orElse(null);
        }

        return null;
    }

    private BidResponseForAdmin mapToAdminResponse(Bid bid) {

        String companyName = resolveCompanyName(bid);
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
                .companyName(companyName)
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

    private String generateClientCode() {
        int year = Year.now().getValue();
        Integer sequence = sequenceService.nextSequence(
                SequenceType.CLIENT,
                year
        );
        return String.format("TWC-CLI-%d-%03d", year, sequence);
    }
}