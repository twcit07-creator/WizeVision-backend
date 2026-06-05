package com.thewizecompany.wizevision.attendance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/*
 * PIN LOGIN REQUEST
 *
 * Used by Windows attendance app for employee authentication.
 *
 * SECURITY DESIGN:
 * Email + PIN (two factors):
 *   Email  → identifies WHO is logging in (unique per employee)
 *   PIN    → proves they are who they say they are
 *
 * Why not just PIN?
 *   4-digit PIN = 10,000 combinations
 *   Someone at the machine could brute-force it
 *   Email + PIN means attacker needs BOTH pieces
 *
 * Why not use portal password?
 *   Portal password is long and complex (8-128 chars)
 *   Impractical to type at a physical attendance machine
 *   PIN is intentionally short for quick daily use
 *   The two systems (portal vs attendance) have
 *   separate credentials intentionally
 */
@Getter
@Setter
public class PinLoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "PIN is required")
    @Pattern(
            regexp = "^[0-9]{4}$",
            message = "PIN must be exactly 4 digits"
    )
    private String pin;

    @NotBlank(message = "Machine identifier is required")
    private String machineIdentifier;

    @NotBlank(message = "Machine name is required")
    private String machineName;

    private String appVersion;
}