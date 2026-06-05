package com.thewizecompany.wizevision.projects.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectPhase {

    /*
     * MODELLING    → Modeler builds the 3D steel model
     * DRAFTING     → Editor generates 2D drawings from model
     * CHECKING     → Checker reviews drawings for errors
     * CORRECTIONS  → Modeler/Editor fixes checker's comments
     * FINAL_REVIEW → PM reviews before sending to client
     * DELIVERED    → Drawings sent to client
     * COMPLETED    → Client accepted the deliverables
     */
    MODELLING("Modelling"),
    DRAFTING("Drafting"),
    CHECKING("Checking"),
    CORRECTIONS("Corrections"),
    FINAL_REVIEW("Final Review"),
    DELIVERED("Delivered"),
    COMPLETED("Completed");

    private final String displayName;
}