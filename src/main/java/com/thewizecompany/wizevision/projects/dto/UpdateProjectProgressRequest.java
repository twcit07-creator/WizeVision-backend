package com.thewizecompany.wizevision.projects.dto;

import com.thewizecompany.wizevision.projects.domain.ProjectPhase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectProgressRequest {

    private ProjectPhase phase;

    @Min(0)
    @Max(100)
    private Integer progressPercentage;

    private String pmNotes;
}