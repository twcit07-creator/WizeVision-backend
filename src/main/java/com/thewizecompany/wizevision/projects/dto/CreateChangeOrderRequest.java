package com.thewizecompany.wizevision.projects.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChangeOrderRequest {

    @NotBlank(message = "Description is required")
    private String description;

    private String scopeOfChange;
    private String notes;
}