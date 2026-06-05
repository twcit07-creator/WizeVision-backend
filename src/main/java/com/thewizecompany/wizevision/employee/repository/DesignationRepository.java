package com.thewizecompany.wizevision.employee.repository;

import com.thewizecompany.wizevision.employee.domain.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DesignationRepository
        extends JpaRepository<Designation, UUID> {

    List<Designation> findByIsActiveTrueAndIsDeletedFalse();

    /*
     * Returns designations for a specific department.
     * Used when frontend filters designations
     * after user selects a department.
     */
    List<Designation> findByDepartment_IdAndIsActiveTrueAndIsDeletedFalse(
            UUID departmentId
    );

    Optional<Designation> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByTitleAndDepartment_IdAndIsDeletedFalse(
            String title,
            UUID departmentId
    );
}