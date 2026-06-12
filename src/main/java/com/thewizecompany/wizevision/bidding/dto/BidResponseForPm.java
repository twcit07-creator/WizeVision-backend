package com.thewizecompany.wizevision.bidding.dto;

import com.thewizecompany.wizevision.bidding.domain.BidStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/*
 * What PM sees — bid amount is NOT included.
 * PM can see everything else.
 */
@Getter
@Builder
public class BidResponseForPm {

    private final UUID id;
    private final String bidNumber;
    private final Integer revisionNumber;

    private final UUID inquiryId;
    private final String inquiryNumber;

    private final UUID clientId;
    private final String clientName;
    private final String clientCode;

    private final UUID clientContactId;
    private final String clientContactName;

    private final String projectName;
    private final String projectLocation;
    private final String scopeOfWork;

    /*
     * Inclusions/exclusions as raw JSON string.
     * Frontend parses and renders as bullet list.
     */
    private final String inclusions;
    private final String exclusions;
    private final String referenceDocuments;

    private final Integer estimatedWeeks;
    private final LocalDate proposedStartDate;
    private final LocalDate proposedEndDate;

    private final BidStatus status;
    private final String statusDisplay;

    /*
     * BID AMOUNT INTENTIONALLY OMITTED.
     * PM cannot see bidAmount or internalNotes.
     */

    private final String notes;
    private final Instant submittedAt;
    private final Instant sentToClientAt;
    private final Instant decidedAt;
    private final String rejectionReason;
    private final UUID convertedProjectId;
    private final Instant createdAt;

    /*
     * Company name shown to PM.
     * For existing clients → comes from client record.
     * For prospects (lead-based) → comes from lead record.
     * PM always sees the company name regardless of
     * whether a client record exists yet.
     */
    private final String companyName;
}