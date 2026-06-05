package com.thewizecompany.wizevision.attendance.service;

import com.thewizecompany.wizevision.attendance.domain.*;
import com.thewizecompany.wizevision.attendance.dto.*;
import com.thewizecompany.wizevision.attendance.repository.AttendanceEventRepository;
import com.thewizecompany.wizevision.attendance.repository.AttendanceRepository;
import com.thewizecompany.wizevision.attendance.repository.MachineSessionRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.domain.Role;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.shared.config.AppProperties;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final int SHIFT_HOURS = 8;
    private static final int LATE_GRACE_MINUTES = 15;

    private final AttendanceRepository attendanceRepository;
    private final AttendanceEventRepository eventRepository;
    private final MachineSessionRepository machineSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;

    // ─────────────────────────────────────────────────────────
// PIN LOGIN (Windows App)
// ─────────────────────────────────────────────────────────

    @Transactional
    public PinLoginResponse pinLogin(PinLoginRequest request) {

        String email = request.getEmail()
                .toLowerCase()
                .trim();

        Employee employee = employeeRepository
                .findByEmailAndIsDeletedFalseAndIsActiveTrue(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or PIN")
                );

        /*
         * ROLE CHECK — Admins do not use the Windows attendance app.
         *
         * SUPER_ADMIN and ADMIN manage the system via the web portal.
         * They do not clock in/out via a Windows attendance machine.
         * Blocking them here prevents:
         * 1. Accidental attendance tracking for admin accounts
         * 2. Any attempt to authenticate high-privilege accounts
         *    through the attendance app
         *
         * If an admin genuinely needs attendance tracking,
         * HR can downgrade their role temporarily or create
         * a separate employee account for attendance.
         */
        if (employee.getRole() == Role.SUPER_ADMIN
                || employee.getRole() == Role.ADMIN) {

            log.warn(
                    "PIN login attempted for admin account: {} " +
                            "from machine: {}",
                    email,
                    request.getMachineIdentifier()
            );

            /*
             * Same generic message — do not reveal that
             * the account exists but is blocked.
             * Attacker should not know if email was valid.
             */
            throw new BadCredentialsException(
                    "Invalid email or PIN"
            );
        }

        if (!employee.isActive()) {
            throw new BadCredentialsException(
                    "Account is deactivated. Contact HR."
            );
        }

        if (employee.isAccountLocked()) {
            throw new BadCredentialsException(
                    "Account is temporarily locked."
            );
        }

        if (!employee.hasAttendancePin()) {
            throw new BadCredentialsException(
                    "Attendance PIN not set. Contact HR."
            );
        }

        if (!passwordEncoder.matches(
                request.getPin(),
                employee.getAttendancePinHash())) {

            employee.recordFailedLogin();
            employeeRepository.save(employee);

            log.warn(
                    "Failed PIN attempt {}/5 for: {}",
                    employee.getFailedLoginAttempts(),
                    email
            );

            throw new BadCredentialsException("Invalid email or PIN");
        }

        employee.recordSuccessfulLogin(null);
        employeeRepository.save(employee);

        String sessionToken = jwtUtil.generateAccessToken(
                employee.getId(),
                employee.getEmail(),
                employee.getRole().name()
        );

        updateMachineSession(
                request.getMachineIdentifier(),
                request.getMachineName(),
                employee.getId(),
                request.getAppVersion()
        );

        log.info(
                "PIN login successful: {} on machine: {}",
                employee.getEmployeeCode(),
                request.getMachineIdentifier()
        );

        return PinLoginResponse.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .department(
                        employee.getDepartment() != null
                                ? employee.getDepartment().getName()
                                : null
                )
                .sessionToken(sessionToken)
                .sessionExpiresInMs(
                        appProperties.getSecurity()
                                .getJwt()
                                .getAccessTokenExpiryMs()
                )
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // REAL-TIME EVENT RECORDING
    // ─────────────────────────────────────────────────────────

    @Transactional
    public AttendanceSummaryResponse recordEvent(
            UUID employeeId,
            AttendanceEventRequest request,
            String machineIdentifier,
            String machineName) {

        LocalDate today = LocalDate.now();
        Instant eventTime = request.getEventTime() != null
                ? request.getEventTime()
                : Instant.now();

        /*
         * Get or create today's attendance record.
         */
        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
                        employeeId, today
                )
                .orElseGet(() ->
                        createNewRecord(employeeId, today,
                                machineIdentifier, machineName)
                );

        /*
         * Process the event based on type.
         */
        processEvent(
                record,
                request.getEventType(),
                eventTime,
                machineIdentifier,
                machineName,
                request.getNotes(),
                false
        );

        AttendanceRecord saved = attendanceRepository.save(record);

        /*
         * Update machine session current status.
         */
        updateMachineStatus(
                machineIdentifier,
                request.getEventType()
        );

        return mapToSummary(saved);
    }

    // ─────────────────────────────────────────────────────────
    // OFFLINE SYNC
    // ─────────────────────────────────────────────────────────

    @Transactional
    public List<String> syncOfflineEvents(
            UUID employeeId,
            OfflineSyncRequest request) {

        List<String> processedLocalIds = new ArrayList<>();

        /*
         * Sort events by time before processing.
         * Offline queue may not be in order.
         */
        List<OfflineSyncRequest.OfflineEvent> sorted =
                request.getEvents()
                        .stream()
                        .sorted(Comparator.comparing(
                                OfflineSyncRequest.OfflineEvent::getEventTime
                        ))
                        .toList();

        for (OfflineSyncRequest.OfflineEvent event : sorted) {
            try {
                LocalDate eventDate = event.getEventTime()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

                AttendanceRecord record = attendanceRepository
                        .findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
                                employeeId, eventDate
                        )
                        .orElseGet(() ->
                                createNewRecord(
                                        employeeId,
                                        eventDate,
                                        request.getMachineIdentifier(),
                                        null
                                )
                        );

                processEvent(
                        record,
                        event.getEventType(),
                        event.getEventTime(),
                        request.getMachineIdentifier(),
                        null,
                        event.getNotes(),
                        true // isOffline = true
                );

                attendanceRepository.save(record);

                if (event.getLocalId() != null) {
                    processedLocalIds.add(event.getLocalId());
                }

            } catch (Exception e) {
                /*
                 * Don't fail the entire sync for one bad event.
                 * Log and continue with the rest.
                 */
                log.error(
                        "Failed to process offline event {} for {}: {}",
                        event.getEventType(),
                        employeeId,
                        e.getMessage()
                );
            }
        }

        log.info(
                "Synced {} offline events for employee: {}",
                processedLocalIds.size(),
                employeeId
        );

        return processedLocalIds;
    }

    // ─────────────────────────────────────────────────────────
    // HEARTBEAT
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void recordHeartbeat(
            UUID employeeId,
            String machineIdentifier,
            String appVersion) {

        machineSessionRepository
                .findByMachineIdentifierAndIsDeletedFalse(
                        machineIdentifier
                )
                .ifPresent(session -> {
                    session.setLastHeartbeat(Instant.now());
                    session.setAppVersion(appVersion);
                    machineSessionRepository.save(session);
                });
    }

    // ─────────────────────────────────────────────────────────
// IDLE START
// Called when Windows app lock screen appears
// ─────────────────────────────────────────────────────────

    @Transactional
    public void recordIdleStart(
            UUID employeeId,
            IdleStartRequest request) {

        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
                        employeeId, today
                )
                .orElseThrow(() -> new BusinessException(
                        "No active session found for today. " +
                                "Please check in first.",
                        "NO_ACTIVE_SESSION"
                ));

        if (record.isDayEnded()) {
            throw new BusinessException(
                    "Your work day has already ended.",
                    "DAY_ENDED"
            );
        }

        if (record.getCheckInTime() == null) {
            throw new BusinessException(
                    "Cannot record idle — not checked in.",
                    "NOT_CHECKED_IN"
            );
        }

        /*
         * Save the IDLE_START event with the corrected time.
         * The Windows app already subtracted 3 minutes.
         * We store exactly what the app sends.
         */
        AttendanceEvent idleStartEvent = AttendanceEvent.builder()
                .employeeId(employeeId)
                .attendanceRecordId(record.getId())
                .eventType(AttendanceEventType.IDLE_START)
                .eventTime(request.getIdleStartActual())
                .idleStartActual(request.getIdleStartActual())
                .machineIdentifier(request.getMachineIdentifier())
                .offline(false)
                .build();

        eventRepository.save(idleStartEvent);

        // Update machine status to IDLE
        updateMachineStatus(
                request.getMachineIdentifier(),
                AttendanceEventType.IDLE_START
        );

        log.info(
                "Idle start recorded for employee: {} at: {}",
                employeeId,
                request.getIdleStartActual()
        );
    }

// ─────────────────────────────────────────────────────────
// IDLE END
// Called when employee returns within 30 minutes
// and selects a reason
// ─────────────────────────────────────────────────────────

    @Transactional
    public AttendanceSummaryResponse recordIdleEnd(
            UUID employeeId,
            IdleEndRequest request) {

        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
                        employeeId, today
                )
                .orElseThrow(() -> new BusinessException(
                        "No active session found.",
                        "NO_ACTIVE_SESSION"
                ));

        if (record.isDayEnded()) {
            throw new BusinessException(
                    "Your work day has already ended.",
                    "DAY_ENDED"
            );
        }

        /*
         * Find the most recent IDLE_START event to calculate duration.
         * This gives us:
         *   idleStartActual = when idle began
         *   returnTime      = now (when employee came back)
         *   duration        = returnTime - idleStartActual
         */
        Instant startOfDay = today
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        AttendanceEvent lastIdleStart = eventRepository
                .findLastEventOfTypeToday(
                        employeeId,
                        AttendanceEventType.IDLE_START,
                        startOfDay
                )
                .orElseThrow(() -> new BusinessException(
                        "No idle start event found.",
                        "NO_IDLE_START"
                ));

        /*
         * Calculate how long employee was away.
         * This is the number shown on the Windows app:
         * "You were away for X minutes"
         */
        long absenceMinutes =
                (request.getReturnTime().getEpochSecond()
                        - lastIdleStart.getIdleStartActual()
                        .getEpochSecond()) / 60;

        /*
         * Add idle time to the daily total.
         */
        record.setTotalIdleMinutes(
                record.getTotalIdleMinutes() + (int) absenceMinutes
        );

        /*
         * Save IDLE_END event with full details.
         */
        AttendanceEvent idleEndEvent = AttendanceEvent.builder()
                .employeeId(employeeId)
                .attendanceRecordId(record.getId())
                .eventType(AttendanceEventType.IDLE_END)
                .eventTime(request.getReturnTime())
                .idleStartActual(lastIdleStart.getIdleStartActual())
                .idleReason(request.getReason())
                .absenceDurationMinutes((int) absenceMinutes)
                .machineIdentifier(request.getMachineIdentifier())
                .offline(false)
                .build();

        eventRepository.save(idleEndEvent);
        AttendanceRecord saved = attendanceRepository.save(record);

        // Resume machine status
        updateMachineStatus(
                request.getMachineIdentifier(),
                AttendanceEventType.IDLE_END
        );

        log.info(
                "Idle end recorded for employee: {} — away {} mins, reason: {}",
                employeeId,
                absenceMinutes,
                request.getReason()
        );

        return mapToSummary(saved);
    }

// ─────────────────────────────────────────────────────────
// AUTO CHECKOUT
// Called after 30 min no-return threshold
// ─────────────────────────────────────────────────────────

    @Transactional
    public void recordAutoCheckout(
            UUID employeeId,
            AutoCheckoutRequest request) {

        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
                        employeeId, today
                )
                .orElseThrow(() -> new BusinessException(
                        "No active session found.",
                        "NO_ACTIVE_SESSION"
                ));

        if (record.isDayEnded()) {
            // Already processed — idempotent, just return
            log.debug(
                    "Auto-checkout called but day already ended: {}",
                    employeeId
            );
            return;
        }

        if (record.getCheckOutTime() != null) {
            // Already checked out — idempotent
            return;
        }

        /*
         * Set checkout time to idle start actual time.
         * This is the FAIR checkout time —
         * the last confirmed time at the desk.
         *
         * NOT the current time (which would be 30 mins later).
         * NOT the detection time (which would be 3 mins late).
         */
        record.setCheckOutTime(request.getCheckoutTime());
        recalculateTotals(
                record,
                today.atStartOfDay().toInstant(ZoneOffset.UTC)
        );

        /*
         * Save AUTO_CHECKOUT event.
         * isAutoCheckout = true distinguishes this from
         * a manual checkout in reports.
         */
        AttendanceEvent autoCheckoutEvent = AttendanceEvent.builder()
                .employeeId(employeeId)
                .attendanceRecordId(record.getId())
                .eventType(AttendanceEventType.CHECK_OUT)
                .eventTime(request.getCheckoutTime())
                .autoCheckout(true)
                .machineIdentifier(request.getMachineIdentifier())
                .notes("Auto-checkout: no return within 30 minutes")
                .offline(false)
                .build();

        eventRepository.save(autoCheckoutEvent);
        attendanceRepository.save(record);

        // Update machine status
        updateMachineStatus(
                request.getMachineIdentifier(),
                AttendanceEventType.CHECK_OUT
        );

        log.info(
                "Auto-checkout recorded for employee: {} at: {}",
                employeeId,
                request.getCheckoutTime()
        );
    }

// ─────────────────────────────────────────────────────────
// RESUME SESSION
// Called when employee re-logs in after auto-checkout
// ─────────────────────────────────────────────────────────

    @Transactional
    public ResumeSessionResponse resumeSession(
            UUID employeeId,
            ResumeSessionRequest request) {

        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
                        employeeId, today
                )
                .orElseThrow(() -> new BusinessException(
                        "No attendance record found for today.",
                        "NO_RECORD"
                ));

        /*
         * Find the auto-checkout time.
         * This is when the absence started.
         * We need it to calculate gap duration.
         */
        Instant absenceStartTime = record.getCheckOutTime();

        if (absenceStartTime == null) {
            throw new BusinessException(
                    "No auto-checkout found. Use idle-end instead.",
                    "NO_AUTO_CHECKOUT"
            );
        }

        /*
         * Calculate how long employee was absent.
         * absenceStart = auto-checkout time (idle start actual)
         * absenceEnd   = now (re-login time)
         */
        long absenceMinutes =
                (request.getResumeTime().getEpochSecond()
                        - absenceStartTime.getEpochSecond()) / 60;

        long absenceHours = absenceMinutes / 60;

        /*
         * THE 4-HOUR RULE:
         * Single continuous absence >= 4 hours = day ended.
         * Employee cannot resume work.
         *
         * We check in hours to avoid edge cases:
         * 239 minutes = 3h 59m = allowed
         * 240 minutes = 4h 00m = day ended
         */
        boolean dayEnded = absenceHours >= 4;

        if (dayEnded) {
            /*
             * Mark day as ended permanently.
             * Employee cannot resume after this.
             */
            record.setDayEnded(true);
            record.setDayEndedAt(request.getResumeTime());
            record.setDayEndedReason(
                    "Single absence exceeded 4 hours"
            );
            attendanceRepository.save(record);

            log.info(
                    "Day ended for employee: {} — absent {} hours {} mins",
                    employeeId,
                    absenceHours,
                    absenceMinutes % 60
            );

            return ResumeSessionResponse.builder()
                    .canResume(false)
                    .autoCheckoutTime(absenceStartTime)
                    .absenceDurationMinutes((int) absenceMinutes)
                    .absenceStartTime(absenceStartTime)
                    .absenceEndTime(request.getResumeTime())
                    .totalWorkMinutesToday(record.getTotalWorkMinutes())
                    .message(
                            "Your work day has ended. " +
                                    "You were absent for " + absenceHours +
                                    " hours " + (absenceMinutes % 60) +
                                    " minutes. See you tomorrow!"
                    )
                    .build();
        }

        /*
         * Gap is less than 4 hours — session can resume.
         *
         * Steps:
         * 1. Clear the auto-checkout (session continues)
         * 2. Add absence duration to idle minutes
         * 3. Save resume event with reason
         * 4. Update machine session
         */
        record.setCheckOutTime(null); // Clear auto-checkout
        record.setTotalIdleMinutes(
                record.getTotalIdleMinutes() + (int) absenceMinutes
        );

        attendanceRepository.save(record);

        /*
         * Save RESUME event.
         * isResumeEvent = true marks this as a re-login
         * after auto-checkout in the timeline view.
         */
        AttendanceEvent resumeEvent = AttendanceEvent.builder()
                .employeeId(employeeId)
                .attendanceRecordId(record.getId())
                .eventType(AttendanceEventType.CHECK_IN)
                .eventTime(request.getResumeTime())
                .idleStartActual(absenceStartTime)
                .idleReason(request.getAbsenceReason())
                .absenceDurationMinutes((int) absenceMinutes)
                .machineIdentifier(request.getMachineIdentifier())
                .machineName(request.getMachineName())
                .resumeEvent(true)
                .notes("Session resumed after " + absenceMinutes +
                        " min absence. Reason: " +
                        request.getAbsenceReason().getDisplayName())
                .offline(false)
                .build();

        eventRepository.save(resumeEvent);

        updateMachineSession(
                request.getMachineIdentifier(),
                request.getMachineName(),
                employeeId,
                null
        );

        updateMachineStatus(
                request.getMachineIdentifier(),
                AttendanceEventType.CHECK_IN
        );

        log.info(
                "Session resumed for employee: {} after {} mins — reason: {}",
                employeeId,
                absenceMinutes,
                request.getAbsenceReason()
        );

        return ResumeSessionResponse.builder()
                .canResume(true)
                .absenceDurationMinutes((int) absenceMinutes)
                .absenceStartTime(absenceStartTime)
                .absenceEndTime(request.getResumeTime())
                .message(
                        "Welcome back! You were away for " +
                                absenceHours + " hour(s) " +
                                (absenceMinutes % 60) + " minute(s). " +
                                "Session resumed."
                )
                .build();
    }

// ─────────────────────────────────────────────────────────
// GET IDLE REASONS (for Windows app dropdown)
// ─────────────────────────────────────────────────────────

    public List<Map<String, String>> getIdleReasons() {
        /*
         * Returns the list of idle reasons for the Windows app.
         * App can call this on startup to populate dropdown.
         * Works offline too since app caches this list locally.
         *
         * Returns:
         * [
         *   { "code": "LUNCH_BREAK",  "display": "Lunch Break" },
         *   { "code": "TEA_BREAK",    "display": "Tea Break" },
         *   ...
         * ]
         */
        return Arrays.stream(IdleReason.values())
                .map(reason -> Map.of(
                        "code", reason.name(),
                        "display", reason.getDisplayName()
                ))
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // LIVE PRESENCE (Web Portal Dashboard)
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LivePresenceResponse> getLivePresence() {

        List<MachineSession> sessions =
                machineSessionRepository.findAllActiveSessions();

        LocalDate today = LocalDate.now();

        return sessions.stream()
                .map(session -> {
                    Employee emp = employeeRepository
                            .findByIdAndIsDeletedFalse(
                                    session.getCurrentEmployeeId()
                            )
                            .orElse(null);

                    if (emp == null) return null;

                    AttendanceRecord record = attendanceRepository
                            .findByEmployeeIdAndAttendanceDateAndIsDeletedFalse(
                                    emp.getId(), today
                            )
                            .orElse(null);

                    return LivePresenceResponse.builder()
                            .employeeId(emp.getId())
                            .employeeCode(emp.getEmployeeCode())
                            .fullName(emp.getFullName())
                            .department(
                                    emp.getDepartment() != null
                                            ? emp.getDepartment().getName()
                                            : null
                            )
                            .role(emp.getRole())
                            .machineIdentifier(
                                    session.getMachineIdentifier()
                            )
                            .machineName(session.getMachineName())
                            .online(session.isOnline())
                            .currentStatus(session.getCurrentStatus())
                            .checkInTime(
                                    record != null
                                            ? record.getCheckInTime()
                                            : null
                            )
                            .totalWorkMinutes(
                                    record != null
                                            ? record.getTotalWorkMinutes()
                                            : 0
                            )
                            .totalBreakMinutes(
                                    record != null
                                            ? record.getTotalBreakMinutes()
                                            : 0
                            )
                            .totalIdleMinutes(
                                    record != null
                                            ? record.getTotalIdleMinutes()
                                            : 0
                            )
                            .lastHeartbeat(session.getLastHeartbeat())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // HISTORY (Web Portal)
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AttendanceSummaryResponse> getHistory(
            UUID employeeId,
            Pageable pageable) {

        return attendanceRepository
                .findByEmployeeIdAndIsDeletedFalse(
                        employeeId, pageable
                )
                .map(this::mapToSummary);
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> getByDateRange(
            UUID employeeId,
            LocalDate from,
            LocalDate to) {

        return attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetweenAndIsDeletedFalse(
                        employeeId, from, to
                )
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────

    private void processEvent(
            AttendanceRecord record,
            AttendanceEventType type,
            Instant eventTime,
            String machineIdentifier,
            String machineName,
            String notes,
            boolean isOffline) {

        Instant startOfDay = record.getAttendanceDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        switch (type) {

            case CHECK_IN -> {
                if (record.getCheckInTime() != null) {
                    log.warn(
                            "Duplicate check-in for employee: {}",
                            record.getEmployeeId()
                    );
                    return;
                }
                record.setCheckInTime(eventTime);
                record.setStatus(AttendanceStatus.PRESENT);
                record.setMachineIdentifier(machineIdentifier);
                record.setMachineName(machineName);
                saveEvent(record, type, eventTime,
                        machineIdentifier, machineName,
                        notes, isOffline);
            }

            case CHECK_OUT -> {
                if (record.getCheckInTime() == null) {
                    throw new BusinessException(
                            "Cannot check out without checking in",
                            "NO_CHECK_IN"
                    );
                }
                record.setCheckOutTime(eventTime);
                recalculateTotals(record, startOfDay);
                saveEvent(record, type, eventTime,
                        machineIdentifier, machineName,
                        notes, isOffline);
            }

            case BREAK_START, IDLE_START -> {
                saveEvent(record, type, eventTime,
                        machineIdentifier, machineName,
                        notes, isOffline);
            }

            case BREAK_END -> {
                /*
                 * Find the last BREAK_START to calculate duration.
                 */
                eventRepository
                        .findLastEventOfTypeToday(
                                record.getEmployeeId(),
                                AttendanceEventType.BREAK_START,
                                startOfDay
                        )
                        .ifPresent(breakStart -> {
                            long breakMinutes =
                                    (eventTime.getEpochSecond()
                                            - breakStart.getEventTime()
                                            .getEpochSecond()) / 60;
                            record.setTotalBreakMinutes(
                                    record.getTotalBreakMinutes()
                                            + (int) breakMinutes
                            );
                        });
                saveEvent(record, type, eventTime,
                        machineIdentifier, machineName,
                        notes, isOffline);
            }

            case IDLE_END -> {
                eventRepository
                        .findLastEventOfTypeToday(
                                record.getEmployeeId(),
                                AttendanceEventType.IDLE_START,
                                startOfDay
                        )
                        .ifPresent(idleStart -> {
                            long idleMinutes =
                                    (eventTime.getEpochSecond()
                                            - idleStart.getEventTime()
                                            .getEpochSecond()) / 60;
                            record.setTotalIdleMinutes(
                                    record.getTotalIdleMinutes()
                                            + (int) idleMinutes
                            );
                        });
                saveEvent(record, type, eventTime,
                        machineIdentifier, machineName,
                        notes, isOffline);
            }

            case HEARTBEAT -> {
                // Heartbeat just updates machine session
                // no record changes needed
            }
        }
    }

    private void saveEvent(
            AttendanceRecord record,
            AttendanceEventType type,
            Instant eventTime,
            String machineIdentifier,
            String machineName,
            String notes,
            boolean isOffline) {

        AttendanceEvent event = AttendanceEvent.builder()
                .employeeId(record.getEmployeeId())
                .attendanceRecordId(record.getId())
                .eventType(type)
                .eventTime(eventTime)
                .machineIdentifier(machineIdentifier)
                .machineName(machineName)
                .notes(notes)
                .offline(isOffline)
                .syncedAt(isOffline ? Instant.now() : null)
                .build();

        eventRepository.save(event);
    }

    private void recalculateTotals(
            AttendanceRecord record,
            Instant startOfDay) {

        if (record.getCheckInTime() == null
                || record.getCheckOutTime() == null) {
            return;
        }

        long totalMinutes =
                (record.getCheckOutTime().getEpochSecond()
                        - record.getCheckInTime().getEpochSecond()) / 60;

        int workMinutes = (int) totalMinutes
                - record.getTotalBreakMinutes();

        record.setTotalWorkMinutes(Math.max(0, workMinutes));

        int expectedMinutes = SHIFT_HOURS * 60;
        if (workMinutes > expectedMinutes) {
            record.setOvertimeMinutes(
                    workMinutes - expectedMinutes
            );
        }
    }

    private AttendanceRecord createNewRecord(
            UUID employeeId,
            LocalDate date,
            String machineIdentifier,
            String machineName) {

        AttendanceRecord record = AttendanceRecord.builder()
                .employeeId(employeeId)
                .attendanceDate(date)
                .status(AttendanceStatus.ABSENT)
                .totalWorkMinutes(0)
                .totalBreakMinutes(0)
                .totalIdleMinutes(0)
                .overtimeMinutes(0)
                .machineIdentifier(machineIdentifier)
                .machineName(machineName)
                .build();

        return attendanceRepository.save(record);
    }

    private void updateMachineSession(
            String machineIdentifier,
            String machineName,
            UUID employeeId,
            String appVersion) {

        MachineSession session = machineSessionRepository
                .findByMachineIdentifierAndIsDeletedFalse(
                        machineIdentifier
                )
                .orElse(MachineSession.builder()
                        .machineIdentifier(machineIdentifier)
                        .build()
                );

        session.setMachineName(machineName);
        session.setCurrentEmployeeId(employeeId);
        session.setSessionStartTime(Instant.now());
        session.setLastHeartbeat(Instant.now());
        session.setCurrentStatus("LOGGED_IN");
        session.setAppVersion(appVersion);

        machineSessionRepository.save(session);
    }

    private void updateMachineStatus(
            String machineIdentifier,
            AttendanceEventType eventType) {

        if (machineIdentifier == null) return;

        String status = switch (eventType) {
            case CHECK_IN, BREAK_END, IDLE_END -> "WORKING";
            case CHECK_OUT  -> "CHECKED_OUT";
            case BREAK_START -> "ON_BREAK";
            case IDLE_START -> "IDLE";
            default         -> null;
        };

        if (status == null) return;

        machineSessionRepository
                .findByMachineIdentifierAndIsDeletedFalse(
                        machineIdentifier
                )
                .ifPresent(session -> {
                    session.setCurrentStatus(status);
                    session.setLastHeartbeat(Instant.now());
                    machineSessionRepository.save(session);
                });
    }


    private AttendanceSummaryResponse mapToSummary(
            AttendanceRecord record) {

        return AttendanceSummaryResponse.builder()
                .id(record.getId())
                .employeeId(record.getEmployeeId())
                .attendanceDate(record.getAttendanceDate())
                .status(record.getStatus())
                .checkInTime(record.getCheckInTime())
                .checkOutTime(record.getCheckOutTime())
                .totalWorkMinutes(record.getTotalWorkMinutes())
                .totalBreakMinutes(record.getTotalBreakMinutes())
                .totalIdleMinutes(record.getTotalIdleMinutes())
                .overtimeMinutes(record.getOvertimeMinutes())
                .machineIdentifier(record.getMachineIdentifier())
                .machineName(record.getMachineName())
                .late(record.isLate())
                .lateByMinutes(record.getLateByMinutes())
                .manuallyAdjusted(record.isManuallyAdjusted())
                .adjustmentNote(record.getAdjustmentNote())
                .build();
    }
}