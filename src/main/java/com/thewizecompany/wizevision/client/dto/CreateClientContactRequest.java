package com.thewizecompany.wizevision.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClientContactRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100)
    private String lastName;

    @Size(max = 100)
    private String designation;

    @Email(message = "Please provide a valid email")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$")
    private String phone;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$")
    private String whatsapp;

    @Size(max = 1000)
    private String notes;

    /*
     * If true, this contact will become the primary contact.
     * Any existing primary contact will be demoted.
     */
    private boolean isPrimary = false;
}