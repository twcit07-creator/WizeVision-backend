package com.thewizecompany.wizevision.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/*
 * Used by HR to assign a 4-digit PIN to an employee
 * for the Windows attendance application.
 *
 * HR sets it. Employee cannot change it themselves.
 * HR can reset it anytime.
 *
 * PIN must be exactly 4 digits.
 * Not sequential: 1234 is technically allowed
 * (we keep it simple, HR is responsible for good PINs).
 */
@Getter
@Setter
public class AttendancePinRequest {

    @NotBlank(message = "PIN is required")
    @Pattern(
            regexp = "^[0-9]{4}$",
            message = "PIN must be exactly 4 digits"
    )
    private String pin;
}