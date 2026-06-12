package com.thewizecompany.wizevision.employee.dto;

import com.thewizecompany.wizevision.employee.domain.Department;
import com.thewizecompany.wizevision.employee.domain.Designation;
import com.thewizecompany.wizevision.employee.domain.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/*
 * CREATE EMPLOYEE REQUEST DTO
 *
 * Used by Admin/HR to create a new employee account.
 *
 * PASSWORD STRATEGY:
 * HR does not set the employee's password.
 * System generates a temporary password automatically.
 * mustChangePassword is set to TRUE.
 * Employee must change it on first login.
 *
 * This means:
 * - HR never knows the employee's password
 * - Employee sets their own password from day one
 * - Secure by design
 */
@Getter
@Setter
public class CreateEmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(
            min = 2,
            max = 100,
            message = "First name must be between 2 and 100 characters"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(
            min = 2,
            max = 100,
            message = "Last name must be between 2 and 100 characters"
    )
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Pattern(
            regexp = "^[+]?[0-9]{7,15}$",
            message = "Please provide a valid phone number"
    )
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    private String departmentId;

    private String designationId;

    private LocalDate joiningDate;
}