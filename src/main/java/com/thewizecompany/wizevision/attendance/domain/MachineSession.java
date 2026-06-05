package com.thewizecompany.wizevision.attendance.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/*
 * MACHINE SESSION
 *
 * Tracks which employee is currently logged into which PC.
 * Updated every time an employee logs in via the Windows app.
 *
 * Used by Admin/PM/TL for the live presence dashboard:
 * "Who is on which machine right now?"
 *
 * One row per machine. Updated in-place when a new
 * employee logs into the same machine.
 *
 * last_heartbeat updated every 5 minutes by Windows app.
 * If last_heartbeat > 10 minutes ago → employee may be offline.
 */
@Entity
@Table(
        name = "machine_sessions",
        indexes = {
                @Index(
                        name = "idx_machine_sessions_identifier",
                        columnList = "machine_identifier",
                        unique = true
                ),
                @Index(
                        name = "idx_machine_sessions_employee",
                        columnList = "current_employee_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineSession extends BaseEntity {

    @Column(
            name = "machine_identifier",
            nullable = false,
            unique = true,
            length = 100
    )
    private String machineIdentifier;

    @Column(name = "machine_name", length = 100)
    private String machineName;

    /*
     * The employee currently logged into this machine.
     * Null if no one is logged in.
     */
    @Column(name = "current_employee_id")
    private UUID currentEmployeeId;

    @Column(name = "session_start_time")
    private Instant sessionStartTime;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    /*
     * Current status of the employee on this machine.
     * Updated in real-time as events come in.
     * WORKING / ON_BREAK / IDLE / CHECKED_OUT
     */
    @Column(
            name = "current_status",
            length = 20
    )
    private String currentStatus;

    @Column(
            name = "app_version",
            length = 20
    )
    private String appVersion;

    /*
     * Is this machine currently considered online?
     * True if last_heartbeat < 10 minutes ago.
     */
    public boolean isOnline() {
        if (lastHeartbeat == null) return false;
        return Instant.now()
                .minusSeconds(600) // 10 minutes
                .isBefore(lastHeartbeat);
    }
}