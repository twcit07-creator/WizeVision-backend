package com.thewizecompany.wizevision.bidding.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/*
 * PM updates a DRAFT bid.
 * Only allowed while status = DRAFT.
 * After submission, PM cannot edit.
 */
@Getter
@Setter
public class UpdateBidRequest {

    private UUID clientContactId;

    @Size(min = 2, max = 255)
    private String projectName;

    @Size(max = 255)
    private String projectLocation;

    private String scopeOfWork;
    private List<String> inclusions;
    private List<String> exclusions;
    private List<CreateBidRequest.ReferenceDocumentDto>
            referenceDocuments;

    @Min(1)
    private Integer estimatedWeeks;

    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
    private String notes;
}