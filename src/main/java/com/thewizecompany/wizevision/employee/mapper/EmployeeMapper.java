package com.thewizecompany.wizevision.employee.mapper;

import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.dto.EmployeeResponse;
import org.springframework.stereotype.Component;

/*
 * EMPLOYEE MAPPER
 *
 * Converts Employee entity → EmployeeResponse DTO.
 *
 * WHY NOT USE MAPSTRUCT HERE?
 * MapStruct generates code at compile time and is
 * excellent for complex mappings.
 * For this simple mapping, a manual mapper is
 * more transparent and easier to debug.
 * We can clearly see which fields are included
 * and which sensitive fields are intentionally excluded.
 *
 * We will use MapStruct for more complex mappings
 * (like Bid with nested objects) later.
 *
 * WHAT IS INTENTIONALLY EXCLUDED:
 * - passwordHash        (never expose)
 * - failedLoginAttempts (internal security detail)
 * - accountLockedUntil  (internal security detail)
 * - lastLoginIp         (privacy sensitive)
 * - isDeleted           (internal soft delete flag)
 * - deletedAt/deletedBy (internal)
 * - version             (internal optimistic lock)
 */
@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) return null;

        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .role(employee.getRole())
                .department(employee.getDepartment()==null?"Not Assigned":employee.getDepartment().getName())
                .designation(employee.getDesignation()==null?"Not Assigned":employee.getDesignation().getTitle())
                .joiningDate(employee.getJoiningDate())
                .profilePhotoUrl(employee.getProfilePhotoUrl())
                .isActive(employee.isActive())
                .mustChangePassword(employee.isMustChangePassword())
                .lastLoginAt(employee.getLastLoginAt())
                .createdAt(employee.getCreatedAt())
                .createdBy(employee.getCreatedBy())
                .updatedAt(employee.getUpdatedAt())
                .updatedBy(employee.getUpdatedBy())
                .build();
    }
}