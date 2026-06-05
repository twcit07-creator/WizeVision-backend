package com.thewizecompany.wizevision.employee.dto;

import com.thewizecompany.wizevision.employee.domain.Department;
import com.thewizecompany.wizevision.employee.domain.Designation;
import com.thewizecompany.wizevision.employee.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/*
 * EMPLOYEE RESPONSE DTO
 *
 * What we return for any employee-related API response.
 *
 * CRITICAL SECURITY NOTE:
 * passwordHash is NEVER included here.
 * failedLoginAttempts is NEVER included here.
 * accountLockedUntil is NEVER included here.
 * lastLoginIp is only visible to ADMIN/SUPER_ADMIN.
 *
 * The mapper controls exactly which fields are copied
 * from entity to this DTO. Sensitive fields are
 * simply not mapped — they never leave the server.
 */
@Getter
@Builder
public class EmployeeResponse {

    private final UUID id;
    private final String employeeCode;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final Role role;
    private final Department department;
    private final Designation designation;
    private final LocalDate joiningDate;
    private final String profilePhotoUrl;
    private final boolean isActive;
    private final boolean mustChangePassword;
    private final Instant lastLoginAt;

    /*
     * Audit fields — useful for admin views
     * to see when the account was created and by whom.
     */
    private final Instant createdAt;
    private final String createdBy;
    private final Instant updatedAt;
    private final String updatedBy;
}