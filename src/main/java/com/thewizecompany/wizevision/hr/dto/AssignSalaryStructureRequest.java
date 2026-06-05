package com.thewizecompany.wizevision.hr.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AssignSalaryStructureRequest {

    @NotNull
    private LocalDate effectiveFrom;

    @NotEmpty(message = "At least one component is required")
    private List<ComponentValue> components;

    private String notes;

    @Getter
    @Setter
    public static class ComponentValue {

        @NotNull
        private UUID componentId;

        /*
         * Override the default value for this employee.
         * For BASIC: the actual salary amount
         * For percentage components: the percentage
         *   (usually same as default, but can differ)
         * For FIXED components: the actual fixed amount
         */
        @NotNull
        private BigDecimal value;
    }
}