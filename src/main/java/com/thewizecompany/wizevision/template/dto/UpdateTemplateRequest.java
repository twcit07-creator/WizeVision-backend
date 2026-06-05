package com.thewizecompany.wizevision.template.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTemplateRequest {

    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private String content;

    @Size(max = 20)
    private String templateVersion;

    private Boolean setAsDefault;
    private Boolean isActive;
    private String previewImageUrl;
}