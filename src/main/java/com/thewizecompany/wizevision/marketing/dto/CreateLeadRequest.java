package com.thewizecompany.wizevision.marketing.dto;

import com.thewizecompany.wizevision.marketing.domain.LeadSource;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLeadRequest {

    @NotBlank(message = "Company name is required")
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

    @NotNull(message = "Lead source is required")
    @Enumerated(EnumType.STRING)
    private LeadSource source;

    private String notes;
}