package com.thewizecompany.wizevision.template.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TemplateType {

    INVOICE("Invoice"),
    BID("Bid Document"),
    PAYSLIP("Payslip"),
    PROJECT_SUMMARY("Project Summary");

    private final String displayName;
}