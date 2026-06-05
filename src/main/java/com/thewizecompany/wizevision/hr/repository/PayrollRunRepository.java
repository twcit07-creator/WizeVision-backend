package com.thewizecompany.wizevision.hr.repository;

import com.thewizecompany.wizevision.hr.domain.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollRunRepository
        extends JpaRepository<PayrollRun, UUID> {

    Optional<PayrollRun> findByMonthAndYearAndIsDeletedFalse(
            Integer month,
            Integer year
    );

    Optional<PayrollRun> findByIdAndIsDeletedFalse(UUID id);

    List<PayrollRun> findByIsDeletedFalseOrderByYearDescMonthDesc();
}