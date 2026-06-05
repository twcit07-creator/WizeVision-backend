package com.thewizecompany.wizevision.hr.repository;

import com.thewizecompany.wizevision.hr.domain.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayslipRepository
        extends JpaRepository<Payslip, UUID> {

    Optional<Payslip> findByEmployeeIdAndMonthAndYearAndIsDeletedFalse(
            UUID employeeId,
            Integer month,
            Integer year
    );

    Optional<Payslip> findByIdAndIsDeletedFalse(UUID id);

    List<Payslip> findByPayrollRunIdAndIsDeletedFalse(
            UUID payrollRunId
    );

    List<Payslip> findByEmployeeIdAndIsDeletedFalseOrderByYearDescMonthDesc(
            UUID employeeId
    );
}