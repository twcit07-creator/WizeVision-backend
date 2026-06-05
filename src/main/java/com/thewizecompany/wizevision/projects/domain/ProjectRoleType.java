package com.thewizecompany.wizevision.projects.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectRoleType {

    /*
     * Role an employee plays on a specific project.
     * Different from their system Role (which is company-wide).
     *
     * An employee with system Role=MODELER can be assigned
     * as MODELER on one project and as EDITOR on another
     * if they have the skills.
     *
     * PM is always the one who created/owns the project.
     */
    PROJECT_MANAGER("Project Manager"),
    MODELER("Modeler"),
    EDITOR("Editor"),
    CHECKER("Checker");

    private final String displayName;
}