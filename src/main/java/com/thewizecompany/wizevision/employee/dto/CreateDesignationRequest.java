package com.thewizecompany.wizevision.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateDesignationRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    /*
     * A designation belongs to a department.
     * Example:
     * Department: Detailing
     * Designations: Senior Modeler, Modeler, Editor, Checker
     *
     * Optional — designation can exist without department.
     */
    private UUID departmentId;
}