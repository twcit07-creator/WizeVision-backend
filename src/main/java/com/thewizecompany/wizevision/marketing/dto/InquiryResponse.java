package com.thewizecompany.wizevision.marketing.dto;

import com.thewizecompany.wizevision.marketing.domain.InquiryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InquiryResponse {

    private final UUID id;
    private final String inquiryNumber;

    private final UUID leadId;
    private final String leadCompanyName;

    private final UUID clientId;
    private final String clientName;
    private final String clientCode;

    private final UUID clientContactId;
    private final String clientContactName;

    private final String projectName;
    private final String projectLocation;
    private final String description;
    private final String documentReferences;

    private final InquiryStatus status;

    private final UUID forwardedToId;
    private final String forwardedToName;
    private final UUID forwardedById;
    private final String forwardedByName;
    private final Instant forwardedAt;
    private final String forwardingNotes;

    private final UUID bidId;
    private final String notes;
    private final Instant createdAt;
    private final String createdBy;
}