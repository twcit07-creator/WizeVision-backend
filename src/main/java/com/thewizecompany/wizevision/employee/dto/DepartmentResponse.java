package com.thewizecompany.wizevision.employee.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DepartmentResponse {

    private final UUID id;
    private final String name;
    private final String description;

    /*
     * We return head employee details flat
     * instead of a nested object.
     * Simpler for frontend dropdowns.
     */
    private final UUID headEmployeeId;
    private final String headEmployeeName;
    private final String headEmployeeCode;

    private final boolean isActive;
    private final Instant createdAt;
    private final String createdBy;
}