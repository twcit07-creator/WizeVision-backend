package com.thewizecompany.wizevision.template.dto;

import com.thewizecompany.wizevision.template.domain.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Template type is required")
    private TemplateType templateType;

    @NotBlank(message = "Template content is required")
    private String content;

    @Size(max = 20)
    private String templateVersion;

    /*
     * If true, this template becomes the default
     * for its type. Previous default is demoted.
     */
    private boolean setAsDefault = false;

    private String previewImageUrl;
}