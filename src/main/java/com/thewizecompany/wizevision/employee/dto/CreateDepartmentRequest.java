package com.thewizecompany.wizevision.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateDepartmentRequest {

    @NotBlank(message = "Department name is required")
    @Size(
            min = 2,
            max = 100,
            message = "Name must be between 2 and 100 characters"
    )
    private String name;

    @Size(max = 500)
    private String description;

    /*
     * Optional — department head can be assigned
     * when creating or updated later.
     * Must be a valid employee UUID.
     */
    private UUID headEmployeeId;
}