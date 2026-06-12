package com.thewizecompany.wizevision.bidding.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * Used by PM to create a bid.
 * Amount is NOT included — only admin fills that.
 */
@Getter
@Setter
public class CreateBidRequest {

    /*
     * If created from an inquiry, inquiryId is provided.
     * Project name and client are pre-filled from inquiry.
     * PM can still modify them.
     *
     * If created without an inquiry (direct bid),
     * inquiryId is null and clientId is required.
     */
    private UUID inquiryId;

    /*
     * Optional when inquiryId is provided.
     * Required when no inquiryId (direct bid).
     */
    private UUID clientId;

    private UUID clientContactId;

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 255)
    private String projectName;

    @Size(max = 255)
    private String projectLocation;

    private String scopeOfWork;

    /*
     * List of inclusion bullet points.
     * Example: ["Shop drawings for all steel members",
     *           "Anchor bolt plans"]
     */
    private List<String> inclusions;

    private List<String> exclusions;

    private List<ReferenceDocumentDto> referenceDocuments;

    @Min(value = 1, message = "Estimated weeks must be at least 1")
    private Integer estimatedWeeks;

    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;

    private String notes;

    @Getter
    @Setter
    public static class ReferenceDocumentDto {
        private String type;
        private String date;
        private String revision;
        private String notes;
    }
}