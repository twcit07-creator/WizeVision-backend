package com.thewizecompany.wizevision.hr.repository;

import com.thewizecompany.wizevision.hr.domain.SalaryComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryComponentRepository
        extends JpaRepository<SalaryComponent, UUID> {

    List<SalaryComponent> findByIsActiveTrueAndIsDeletedFalse();

    Optional<SalaryComponent> findByCodeAndIsDeletedFalse(
            String code
    );

    Optional<SalaryComponent> findByIdAndIsDeletedFalse(
            UUID id
    );
}