package com.thewizecompany.wizevision.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateClientContactRequest {

    @Size(min = 2, max = 100)
    private String firstName;

    @Size(min = 2, max = 100)
    private String lastName;

    @Size(max = 100)
    private String designation;

    @Email
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$")
    private String phone;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$")
    private String whatsapp;

    @Size(max = 1000)
    private String notes;

    private Boolean isPrimary;
}