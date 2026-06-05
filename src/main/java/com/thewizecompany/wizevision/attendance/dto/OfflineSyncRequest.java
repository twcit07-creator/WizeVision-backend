package com.thewizecompany.wizevision.attendance.dto;

import com.thewizecompany.wizevision.attendance.domain.AttendanceEventType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/*
 * Sent by Windows app when internet is restored.
 * Contains all events that happened while offline.
 * Server processes them in chronological order.
 *
 * The Windows app stores events in local SQLite.
 * When connected, it sends all PENDING events here.
 * After successful sync, marks them as SYNCED locally.
 */
@Getter
@Setter
public class OfflineSyncRequest {

    @NotNull
    private String machineIdentifier;

    @NotEmpty(message = "Events list cannot be empty")
    private List<OfflineEvent> events;

    @Getter
    @Setter
    public static class OfflineEvent {

        @NotNull
        private AttendanceEventType eventType;

        @NotNull(message = "Event timestamp is required")
        private Instant eventTime;

        /*
         * Local ID from the Windows app's SQLite.
         * Returned in sync response so app knows
         * which local records were processed.
         */
        private String localId;

        private String notes;
    }
}