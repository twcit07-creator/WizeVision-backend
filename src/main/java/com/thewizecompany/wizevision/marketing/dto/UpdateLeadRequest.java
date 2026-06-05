package com.thewizecompany.wizevision.marketing.dto;

import com.thewizecompany.wizevision.marketing.domain.LeadSource;
import com.thewizecompany.wizevision.marketing.domain.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLeadRequest {

    @Size(min = 2, max = 255)
    private String companyName;

    private String industryType;
    private String city;
    private String state;
    private String country;

    @Size(max = 200)
    private String contactName;

    @Email
    private String contactEmail;

    private String contactPhone;
    private String contactWhatsapp;
    private String contactDesignation;
    private LeadSource source;
    private LeadStatus status;
    private String notes;
    private String lostReason;
}