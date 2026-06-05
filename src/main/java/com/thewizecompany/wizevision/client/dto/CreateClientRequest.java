package com.thewizecompany.wizevision.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClientRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 255)
    private String companyName;

    @Email(message = "Please provide a valid email")
    private String email;

    @Pattern(
            regexp = "^[+]?[0-9]{7,15}$",
            message = "Please provide a valid phone number"
    )
    private String phone;

    private String website;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String gstNumber;
    private String industryType;
    private String notes;
}