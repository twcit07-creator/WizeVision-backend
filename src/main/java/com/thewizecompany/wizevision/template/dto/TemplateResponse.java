package com.thewizecompany.wizevision.template.dto;

import com.thewizecompany.wizevision.template.domain.TemplateType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TemplateResponse {

    private final UUID id;
    private final String name;
    private final String description;
    private final TemplateType templateType;
    private final String typeDisplay;
    private final String templateVersion;
    private final boolean isDefault;
    private final boolean isActive;
    private final String previewImageUrl;
    private final Instant createdAt;
    private final String createdBy;
    private final Instant updatedAt;

    /*
     * Content is NOT returned in list responses.
     * Only returned when fetching a single template
     * for editing or preview.
     */
    private final String content;
}