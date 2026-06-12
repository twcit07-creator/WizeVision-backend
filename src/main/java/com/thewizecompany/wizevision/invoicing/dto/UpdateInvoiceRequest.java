package com.thewizecompany.wizevision.invoicing.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdateInvoiceRequest {

    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @DecimalMin(value = "0.01")
    private BigDecimal percentage;

    private String note;
    private LocalDate dueDate;
}
