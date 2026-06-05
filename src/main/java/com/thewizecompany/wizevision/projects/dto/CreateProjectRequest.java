package com.thewizecompany.wizevision.projects.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * Used internally when auto-creating a project
 * from an accepted bid.
 * Not exposed as a public API endpoint —
 * projects are ALWAYS created from accepted bids.
 *
 * For manual project creation (edge case),
 * Admin can use this via a separate endpoint.
 */
@Getter
@Setter
public class CreateProjectRequest {

    private UUID bidId;
    private UUID clientId;
    private UUID clientContactId;
    private String projectName;
    private String projectLocation;
    private String scopeOfWork;
    private String inclusions;
    private String exclusions;
    private BigDecimal contractAmount;
    private UUID pmId;
    private LocalDate estimatedStartDate;
    private LocalDate estimatedEndDate;
    private Integer estimatedWeeks;
}