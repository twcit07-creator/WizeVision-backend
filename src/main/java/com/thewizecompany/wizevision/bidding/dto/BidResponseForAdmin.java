package com.thewizecompany.wizevision.bidding.dto;

import com.thewizecompany.wizevision.bidding.domain.BidStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/*
 * What Admin sees — includes bid amount and internal notes.
 */
@Getter
@Builder
public class BidResponseForAdmin {

    private final UUID id;
    private final String bidNumber;
    private final Integer revisionNumber;

    private final UUID inquiryId;
    private final String inquiryNumber;

    private final UUID clientId;
    private final String clientName;
    private final String clientCode;
    private final String clientEmail;
    private final String clientPhone;

    private final UUID clientContactId;
    private final String clientContactName;
    private final String clientContactEmail;
    private final String clientContactPhone;

    private final String projectName;
    private final String projectLocation;
    private final String scopeOfWork;
    private final String inclusions;
    private final String exclusions;
    private final String referenceDocuments;

    private final Integer estimatedWeeks;
    private final LocalDate proposedStartDate;
    private final LocalDate proposedEndDate;

    /*
     * ADMIN ONLY FIELDS:
     */
    private final BigDecimal bidAmount;
    private final String internalNotes;

    private final BidStatus status;
    private final String statusDisplay;

    private final UUID createdByPmId;
    private final String createdByPmName;

    private final String notes;
    private final Instant submittedAt;
    private final Instant sentToClientAt;
    private final Instant decidedAt;
    private final String rejectionReason;
    private final UUID convertedProjectId;
    private final Instant createdAt;
    private final String createdBy;

    private final String companyName;
}