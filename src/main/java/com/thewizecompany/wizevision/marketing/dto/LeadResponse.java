package com.thewizecompany.wizevision.marketing.dto;

import com.thewizecompany.wizevision.marketing.domain.LeadSource;
import com.thewizecompany.wizevision.marketing.domain.LeadStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class LeadResponse {

    private final UUID id;
    private final String leadNumber;
    private final String companyName;
    private final String industryType;
    private final String city;
    private final String state;
    private final String country;
    private final String contactName;
    private final String contactEmail;
    private final String contactPhone;
    private final String contactWhatsapp;
    private final String contactDesignation;
    private final LeadSource source;
    private final LeadStatus status;
    private final String notes;
    private final String lostReason;
    private final UUID assignedToId;
    private final String assignedToName;
    private final UUID clientId;
    private final String clientName;
    private final Instant convertedAt;
    private final Instant createdAt;
    private final String createdBy;
}