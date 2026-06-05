package com.thewizecompany.wizevision.attendance.controller;

import com.thewizecompany.wizevision.attendance.dto.*;
import com.thewizecompany.wizevision.attendance.service.AttendanceService;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@Tag(
        name = "Attendance",
        description = "Attendance tracking — Windows app and web portal"
)
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeRepository employeeRepository;
    // ─────────────────────────────────────────────────────────
    // WINDOWS APP ENDPOINTS
    // ─────────────────────────────────────────────────────────

    /*
     * PIN LOGIN
     * Called by Windows app when employee enters PIN.
     * No JWT required — PIN is the authentication.
     * Returns a session token for subsequent requests.
     */
    @PostMapping("/auth/pin-login")
    @Operation(
            summary = "Windows App — Login with PIN",
            description = "Authenticates employee via 4-digit PIN. " +
                    "Returns session token for the Windows app."
    )
    public ResponseEntity<ApiResponse<PinLoginResponse>> pinLogin(
            @Valid @RequestBody PinLoginRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        attendanceService.pinLogin(request),
                        "Login successful"
                )
        );
    }

    /*
     * RECORD EVENT
     * Called by Windows app for each attendance event.
     * Requires JWT session token from pin-login.
     */
    @PostMapping("/event")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Windows App — Record attendance event",
            description = "Records CHECK_IN, CHECK_OUT, BREAK_START, " +
                    "BREAK_END, IDLE_START, IDLE_END events."
    )
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>>
    recordEvent(
            @Valid @RequestBody AttendanceEventRequest request,
            @AuthenticationPrincipal String employeeEmail,
            @RequestHeader(
                    value = "X-Machine-Id",
                    required = false
            ) String machineId,
            @RequestHeader(
                    value = "X-Machine-Name",
                    required = false
            ) String machineName) {

        UUID employeeId = resolveEmployeeId(employeeEmail);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        attendanceService.recordEvent(
                                employeeId, request,
                                machineId, machineName
                        ),
                        "Event recorded"
                )
        );
    }

    /*
     * HEARTBEAT
     * Windows app sends this every 5 minutes.
     * Keeps the live presence dashboard accurate.
     * If heartbeat stops → employee marked as offline.
     */
    @PostMapping("/heartbeat")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Windows App — Heartbeat ping",
            description = "Send every 5 minutes to maintain live presence."
    )
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @AuthenticationPrincipal String employeeEmail,
            @RequestHeader(
                    value = "X-Machine-Id",
                    required = false
            ) String machineId,
            @RequestHeader(
                    value = "X-App-Version",
                    required = false
            ) String appVersion) {

        UUID employeeId = resolveEmployeeId(employeeEmail);
        attendanceService.recordHeartbeat(
                employeeId, machineId, appVersion
        );

        return ResponseEntity.ok(
                ApiResponse.ok("Heartbeat received")
        );
    }

    /*
     * OFFLINE SYNC
     * Called when internet is restored.
     * Sends all queued events from local SQLite.
     */
    @PostMapping("/sync")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Windows App — Sync offline events",
            description = "Sends all events queued while offline. " +
                    "Returns list of processed local IDs."
    )
    public ResponseEntity<ApiResponse<List<String>>> sync(
            @Valid @RequestBody OfflineSyncRequest request,
            @AuthenticationPrincipal String employeeEmail) {

        UUID employeeId = resolveEmployeeId(employeeEmail);

        List<String> processed =
                attendanceService.syncOfflineEvents(
                        employeeId, request
                );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        processed,
                        "Synced " + processed.size() + " events"
                )
        );
    }

    // ─────────────────────────────────────────────────────────
// IDLE START
// ─────────────────────────────────────────────────────────

    @PostMapping("/idle-start")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Windows App — Record idle start",
            description = "Called when lock screen appears. " +
                    "idleStartActual = last input time " +
                    "(detection time minus 3 minutes)."
    )
    public ResponseEntity<ApiResponse<Void>> idleStart(
            @Valid @RequestBody IdleStartRequest request,
            @AuthenticationPrincipal String employeeEmail) {

        UUID employeeId = resolveEmployeeId(employeeEmail);
        attendanceService.recordIdleStart(employeeId, request);

        return ResponseEntity.ok(
                ApiResponse.ok("Idle start recorded")
        );
    }

// ─────────────────────────────────────────────────────────
// IDLE END
// ─────────────────────────────────────────────────────────

    @PostMapping("/idle-end")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Windows App — Record idle end with reason",
            description = "Called when employee returns within " +
                    "30 minutes and selects absence reason."
    )
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>>
    idleEnd(
            @Valid @RequestBody IdleEndRequest request,
            @AuthenticationPrincipal String employeeEmail) {

        UUID employeeId = resolveEmployeeId(employeeEmail);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        attendanceService.recordIdleEnd(
                                employeeId, request
                        ),
                        "Session resumed"
                )
        );
    }

// ─────────────────────────────────────────────────────────
// AUTO CHECKOUT
// ─────────────────────────────────────────────────────────

    @PostMapping("/auto-checkout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Windows App — Auto checkout after 30 min",
            description = "Called when employee does not return " +
                    "within 30 minutes of idle detection. " +
                    "checkoutTime should be idleStartActual, " +
                    "NOT the current time."
    )
    public ResponseEntity<ApiResponse<Void>> autoCheckout(
            @Valid @RequestBody AutoCheckoutRequest request,
            @AuthenticationPrincipal String employeeEmail) {

        UUID employeeId = resolveEmployeeId(employeeEmail);
        attendanceService.recordAutoCheckout(employeeId, request);

        return ResponseEntity.ok(
                ApiResponse.ok("Auto-checkout recorded")
        );
    }

// ─────────────────────────────────────────────────────────
// RESUME SESSION
// ─────────────────────────────────────────────────────────

    @PostMapping("/resume")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Windows App — Resume after auto-checkout",
            description = "Called when employee re-logs in after " +
                    "auto-checkout. Returns canResume flag. " +
                    "If canResume=false, day has ended " +
                    "(absence >= 4 hours)."
    )
    public ResponseEntity<ApiResponse<ResumeSessionResponse>>
    resume(
            @Valid @RequestBody ResumeSessionRequest request,
            @AuthenticationPrincipal String employeeEmail) {

        UUID employeeId = resolveEmployeeId(employeeEmail);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        attendanceService.resumeSession(
                                employeeId, request
                        )
                )
        );
    }

// ─────────────────────────────────────────────────────────
// GET IDLE REASONS (Windows app startup)
// ─────────────────────────────────────────────────────────

    @GetMapping("/idle-reasons")
    @Operation(
            summary = "Windows App — Get idle reason list",
            description = "Returns hardcoded list of idle reasons. " +
                    "Windows app caches this locally on startup."
    )
    public ResponseEntity<ApiResponse<List<Map<String, String>>>>
    getIdleReasons() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        attendanceService.getIdleReasons()
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // WEB PORTAL ENDPOINTS
    // ─────────────────────────────────────────────────────────

    /*
     * LIVE PRESENCE DASHBOARD
     * Used by Admin/PM/TL to see who is working right now.
     */
    @GetMapping("/live")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN'," +
                    "'PROJECT_MANAGER','HR_MANAGER','TEAM_LEAD')"
    )
    @Operation(
            summary = "Web Portal — Live presence dashboard",
            description = "Shows all currently active employees, " +
                    "their machine, and current status."
    )
    public ResponseEntity<ApiResponse<List<LivePresenceResponse>>>
    getLivePresence() {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        attendanceService.getLivePresence()
                )
        );
    }

    /*
     * MY ATTENDANCE HISTORY
     * Any employee can view their own history.
     */
    @GetMapping("/my/history")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Web Portal — My attendance history")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceSummaryResponse>>>
    getMyHistory(
            @AuthenticationPrincipal String employeeEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        UUID employeeId = resolveEmployeeId(employeeEmail);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        PageResponse.from(
                                attendanceService.getHistory(
                                        employeeId,
                                        PageRequest.of(
                                                page, size,
                                                Sort.by("attendanceDate").descending()
                                        )
                                )
                        )
                )
        );
    }

    /*
     * EMPLOYEE ATTENDANCE HISTORY (for managers/HR)
     */
    @GetMapping("/{employeeId}/history")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN'," +
                    "'PROJECT_MANAGER','HR_MANAGER')"
    )
    @Operation(
            summary = "Web Portal — Employee attendance history"
    )
    public ResponseEntity<ApiResponse<PageResponse<AttendanceSummaryResponse>>>
    getEmployeeHistory(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        PageResponse.from(
                                attendanceService.getHistory(
                                        employeeId,
                                        PageRequest.of(
                                                page, size,
                                                Sort.by("attendanceDate").descending()
                                        )
                                )
                        )
                )
        );
    }

    /*
     * DATE RANGE REPORT
     */
    @GetMapping("/{employeeId}/range")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','HR_MANAGER')"
    )
    @Operation(
            summary = "Web Portal — Attendance by date range"
    )
    public ResponseEntity<ApiResponse<List<AttendanceSummaryResponse>>>
    getByRange(
            @PathVariable UUID employeeId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        attendanceService.getByDateRange(
                                employeeId, from, to
                        )
                )
        );
    }

    // ─────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────

    private UUID resolveEmployeeId(String email) {
        /*
         * JwtAuthFilter sets the email as principal.
         * We need UUID for service calls.
         * Load employee by email to get UUID.
         *
         * This is cached in Redis in production
         * to avoid DB hit on every request.
         * For now direct DB lookup is fine.
         */
        return employeeRepository == null
                ? null
                : employeeRepository
                  .findByEmailAndIsDeletedFalseAndIsActiveTrue(email)
                  .map(BaseEntity::getId)
                  .orElseThrow(() ->
                               new ResourceNotFoundException(
                                       "Employee", email
                               )
                  );
    }
}