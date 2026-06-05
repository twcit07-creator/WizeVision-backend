package com.thewizecompany.wizevision.attendance.repository;

import com.thewizecompany.wizevision.attendance.domain.MachineSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MachineSessionRepository
        extends JpaRepository<MachineSession, UUID> {

    Optional<MachineSession> findByMachineIdentifierAndIsDeletedFalse(
            String machineIdentifier
    );

    Optional<MachineSession> findByCurrentEmployeeIdAndIsDeletedFalse(
            UUID employeeId
    );

    /*
     * All active sessions — used for live presence dashboard.
     * Shows all machines with someone currently logged in.
     */
    @Query("""
        SELECT s FROM MachineSession s
        WHERE s.currentEmployeeId IS NOT NULL
        AND s.isDeleted = FALSE
        ORDER BY s.lastHeartbeat DESC
        """)
    List<MachineSession> findAllActiveSessions();
}