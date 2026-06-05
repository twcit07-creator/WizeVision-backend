package com.thewizecompany.wizevision.marketing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/*
 * Used when a lead converts to a client
 * and a project inquiry is created.
 *
 * existingClientId: if the fabricator is already
 *   in our clients table, link to them.
 *   If null, a new client is created from lead data.
 */
@Getter
@Setter
public class ConvertLeadRequest {

    /*
     * Optional — if provided, links to existing client.
     * If null, creates a new client from lead data.
     */
    private UUID existingClientId;

    @NotNull(message = "Project inquiry details are required")
    private CreateInquiryRequest inquiry;
}