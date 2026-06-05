package com.thewizecompany.wizevision.marketing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/*
 * Marketing forwards an inquiry to a specific PM.
 *
 * forwardedToId: UUID of the Project Manager employee.
 * notes: any handover notes from marketing to PM.
 */
@Getter
@Setter
public class ForwardInquiryRequest {

    @NotNull(message = "Please select a Project Manager")
    private UUID forwardedToId;

    private String notes;
}