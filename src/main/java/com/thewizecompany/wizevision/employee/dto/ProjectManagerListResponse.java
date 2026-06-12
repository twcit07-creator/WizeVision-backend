package com.thewizecompany.wizevision.employee.dto;

import lombok.Builder;

import java.util.UUID;


@Builder
public record ProjectManagerListResponse(
        UUID id,
        String name
) {
}
