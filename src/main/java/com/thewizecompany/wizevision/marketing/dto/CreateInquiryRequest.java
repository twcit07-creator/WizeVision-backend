package com.thewizecompany.wizevision.marketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/*
 * CREATE INQUIRY REQUEST
 *
 * Used in two scenarios:
 *
 * SCENARIO A — From a lead (prospect):
 *   leadId    = the lead UUID
 *   clientId  = null (company is not yet a client)
 *
 * SCENARIO B — Direct from existing client:
 *   clientId  = the client UUID
 *   leadId    = null (came directly, no lead pipeline)
 *
 * Custom validation ensures at least one is provided.
 * The service layer enforces the business rules for each path.
 */
@Getter
@Setter
public class CreateInquiryRequest {

    /*
     * Optional — provide when inquiry comes from a lead.
     * If provided, lead status → CONVERTED automatically.
     */
    private UUID leadId;

    /*
     * Optional — provide when an existing client
     * has a new project (direct inquiry).
     * If provided, leadId must be null.
     */
    private UUID clientId;

    private UUID clientContactId;

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 255)
    private String projectName;

    @Size(max = 255)
    private String projectLocation;

    private String description;

    private List<DocumentReference> documentReferences;

    private String notes;

    @Getter
    @Setter
    public static class DocumentReference {
        private String type;
        private String date;
        private String revision;
        private String notes;
    }
}
