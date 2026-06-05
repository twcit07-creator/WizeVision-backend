package com.thewizecompany.wizevision.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ClientResponse {

    private final UUID id;
    private final String companyCode;
    private final String companyName;
    private final String email;
    private final String phone;
    private final String website;
    private final String addressLine1;
    private final String addressLine2;
    private final String city;
    private final String state;
    private final String country;
    private final String pincode;
    private final String gstNumber;
    private final String industryType;
    private final String notes;
    private final boolean isActive;
    private final Instant createdAt;
    private final String createdBy;

    /*
     * Contacts included in client response.
     * Frontend can show them in the client detail view
     * without making a separate API call.
     */
    private final List<ClientContactResponse> contacts;

    /*
     * Quick stats shown on client card.
     */
    private final long totalProjects;
    private final long totalBids;
}