package com.thewizecompany.wizevision.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveActionRequest {

    @NotNull
    private LeaveAction action;

    private String remarks;

    public enum LeaveAction {
        APPROVE,
        REJECT
    }
}