package com.thewizecompany.wizevision.employee.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DesignationResponse {

    private final UUID id;
    private final String title;
    private final String description;
    private final UUID departmentId;
    private final String departmentName;
    private final boolean isActive;
    private final Instant createdAt;
}