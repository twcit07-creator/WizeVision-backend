package com.thewizecompany.wizevision.hr.repository;

import com.thewizecompany.wizevision.hr.domain.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository
        extends JpaRepository<LeaveBalance, UUID> {

    List<LeaveBalance> findByEmployeeIdAndYearAndIsDeletedFalse(
            UUID employeeId,
            Integer year
    );

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYearAndIsDeletedFalse(
            UUID employeeId,
            UUID leaveTypeId,
            Integer year
    );
}