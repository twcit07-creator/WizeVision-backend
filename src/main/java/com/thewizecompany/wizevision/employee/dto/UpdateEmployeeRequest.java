package com.thewizecompany.wizevision.employee.dto;

import com.thewizecompany.wizevision.employee.domain.Department;
import com.thewizecompany.wizevision.employee.domain.Designation;
import com.thewizecompany.wizevision.employee.domain.Role;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/*
 * UPDATE EMPLOYEE REQUEST DTO
 *
 * All fields are optional — only provided fields are updated.
 * This is the PATCH pattern — partial updates.
 *
 * Why not reuse CreateEmployeeRequest?
 * - Create requires email and role (mandatory)
 * - Update should not allow email changes
 *   (email is used for login — changing it breaks auth)
 * - Different validation rules for each operation
 * - Separate DTOs = clearer intent, safer code
 */
@Getter
@Setter
public class UpdateEmployeeRequest {

    @Size(min = 2, max = 100)
    private String firstName;

    @Size(min = 2, max = 100)
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$",
            message = "Please provide a valid phone number")
    private String phone;

    /*
     * Role can be changed by ADMIN/SUPER_ADMIN only.
     * Service layer enforces this with @PreAuthorize.
     */
    private Role role;

    @Enumerated(EnumType.STRING)
    private Department department;

    @Enumerated(EnumType.STRING)
    private Designation designation;

    private LocalDate joiningDate;

    private String profilePhotoUrl;
}