package com.thewizecompany.wizevision.employee.repository;

import com.thewizecompany.wizevision.employee.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository
        extends JpaRepository<Department, UUID> {

    /*
     * Used for duplicate name check on create.
     */
    boolean existsByNameAndIsDeletedFalse(String name);

    /*
     * Returns all active departments.
     * Used by frontend dropdowns when creating employees.
     */
    List<Department> findByIsActiveTrueAndIsDeletedFalse();

    Optional<Department> findByIdAndIsDeletedFalse(UUID id);

    /*
     * Used to validate headEmployeeId exists
     * before assigning as department head.
     */
    Optional<Department> findByHeadEmployeeIdAndIsDeletedFalse(
            UUID headEmployeeId
    );
}