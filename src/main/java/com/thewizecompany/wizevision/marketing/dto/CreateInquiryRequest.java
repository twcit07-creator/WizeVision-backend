package com.thewizecompany.wizevision.marketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateInquiryRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 255)
    private String projectName;

    @Size(max = 255)
    private String projectLocation;

    private String description;

    /*
     * Drawing and document references.
     * Marketing team enters dates from the documents
     * the fabricator shared.
     *
     * Example:
     * [
     *   { "type": "Structural Drawing",
     *     "date": "2026-01-10",
     *     "revision": "Rev A" },
     *   { "type": "Architectural Drawing",
     *     "date": "2026-01-08" }
     * ]
     */
    private List<DocumentReference> documentReferences;

    private UUID clientId;
    private UUID clientContactId;
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