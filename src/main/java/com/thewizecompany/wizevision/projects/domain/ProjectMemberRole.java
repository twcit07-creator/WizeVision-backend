package com.thewizecompany.wizevision.projects.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectMemberRole {
    PROJECT_MANAGER("Project Manager"),
    TEAM_LEAD("Team Lead"),
    MODELER("Modeler"),
    EDITOR("Editor"),
    CHECKER("Checker");

    private final String displayName;
}