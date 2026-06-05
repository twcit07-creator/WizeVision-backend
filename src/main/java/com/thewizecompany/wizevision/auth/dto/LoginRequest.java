package com.thewizecompany.wizevision.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/*
 * LOGIN REQUEST DTO
 *
 * What the frontend sends to /api/v1/auth/login
 *
 * WHY VALIDATION ANNOTATIONS HERE AND NOT IN SERVICE?
 *
 * Validation at the DTO level means:
 * - Invalid requests are rejected before hitting service layer
 * - Service code stays clean — no null checks needed
 * - Error messages are consistent and automatic
 * - @Valid in controller triggers these automatically
 *
 * @NotBlank vs @NotNull:
 * @NotNull rejects null only — empty string "" passes
 * @NotBlank rejects null AND empty AND whitespace-only
 * Always use @NotBlank for string fields
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(
            min = 8,
            max = 128,
            message = "Password must be between 8 and 128 characters"
    )
    private String password;
}