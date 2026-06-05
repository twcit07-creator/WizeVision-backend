package com.thewizecompany.wizevision.employee.domain;

public enum SalaryStructure {
    /*
     * MONTHLY: Fixed monthly salary
     * Used for: Full-time employees
     * Payslip = fixed amount per month
     *
     * DAILY: Paid per working day
     * Used for: Contract workers
     * Payslip = daily rate × working days
     *
     * HOURLY: Paid per hour worked
     * Used for: Freelancers
     * Payslip = hourly rate × hours worked
     */
    MONTHLY,
    DAILY,
    HOURLY
}