package com.thewizecompany.wizevision.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/*
 * Lightweight response for lists and dropdowns.
 * Does not include contacts or stats.
 * Used in bid creation form:
 * "Select client" dropdown shows these.
 */
@Getter
@Builder
public class ClientSummaryResponse {

    private final UUID id;
    private final String companyCode;
    private final String companyName;
    private final String city;
    private final String phone;
    private final String email;
    private final boolean isActive;

    /*
     * Primary contact — shown next to client name
     * in dropdowns for quick reference.
     */
    private final String primaryContactName;
    private final String primaryContactPhone;
}