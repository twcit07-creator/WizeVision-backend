package com.thewizecompany.wizevision.projects.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChangeOrderStatus {

    DRAFT("Draft"),
    SUBMITTED("Submitted to Admin"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;
}