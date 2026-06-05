package com.thewizecompany.wizevision.hr.repository;

import com.thewizecompany.wizevision.hr.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTypeRepository
        extends JpaRepository<LeaveType, UUID> {

    List<LeaveType> findByIsActiveTrueAndIsDeletedFalse();

    Optional<LeaveType> findByCodeAndIsDeletedFalse(
            String code
    );

    Optional<LeaveType> findByIdAndIsDeletedFalse(UUID id);
}