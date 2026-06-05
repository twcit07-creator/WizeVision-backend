package com.thewizecompany.wizevision.hr.repository;

import com.thewizecompany.wizevision.hr.domain.EmployeeSalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeSalaryStructureRepository
        extends JpaRepository<EmployeeSalaryStructure, UUID> {

    /*
     * Get the currently active structure for an employee.
     */
    Optional<EmployeeSalaryStructure>
    findByEmployeeIdAndIsActiveTrueAndIsDeletedFalse(
            UUID employeeId
    );

    List<EmployeeSalaryStructure>
    findByEmployeeIdAndIsDeletedFalseOrderByEffectiveFromDesc(
            UUID employeeId
    );
}