package com.thewizecompany.wizevision.employee.service;

import com.thewizecompany.wizevision.auth.repository.RefreshTokenRepository;
import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.domain.Role;
import com.thewizecompany.wizevision.employee.dto.AttendancePinRequest;
import com.thewizecompany.wizevision.employee.dto.CreateEmployeeRequest;
import com.thewizecompany.wizevision.employee.dto.EmployeeResponse;
import com.thewizecompany.wizevision.employee.dto.UpdateEmployeeRequest;
import com.thewizecompany.wizevision.employee.mapper.EmployeeMapper;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import com.thewizecompany.wizevision.shared.exception.BusinessException;
import com.thewizecompany.wizevision.shared.exception.DuplicateResourceException;
import com.thewizecompany.wizevision.shared.exception.ResourceNotFoundException;
import com.thewizecompany.wizevision.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeMapper employeeMapper;

    // ─────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse createEmployee(
            CreateEmployeeRequest request) {

        /*
         * Check for duplicate email before doing anything else.
         * Fail fast — no point continuing if email is taken.
         */
        String email = request.getEmail()
                .toLowerCase()
                .trim();

        if (employeeRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "Employee", "email", email
            );
        }

        /*
         * Generate employee code: WV-2025-001
         * Format: WV-{year}-{sequence padded to 3 digits}
         *
         * We count existing codes with same year prefix
         * to determine the next sequence number.
         */
        String employeeCode = generateEmployeeCode();

        /*
         * Generate temporary password.
         * Pattern: First name + @ + random 4 digits + !
         * Example: John@4821!
         *
         * mustChangePassword = true forces change on first login.
         * In a real system you would email this to the employee.
         * For now, we return it in the response (admin sees it once).
         */
        String temporaryPassword = generateTemporaryPassword(
                request.getFirstName()
        );

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(request.getPhone())
                .passwordHash(
                        passwordEncoder.encode(temporaryPassword)
                )
                .role(request.getRole())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .joiningDate(
                        request.getJoiningDate() != null
                                ? request.getJoiningDate()
                                : LocalDate.now()
                )
                .isActive(true)
                .mustChangePassword(true)
                .build();

        Employee saved = employeeRepository.save(employee);

        log.info(
                "Employee created: {} ({}) with role: {}",
                saved.getEmployeeCode(),
                saved.getEmail(),
                saved.getRole()
        );

        /*
         * TODO: Send welcome email with temporary password
         * This will be implemented in the notification module.
         * For now, log it (dev only — remove in production).
         */
        log.info(
                "Temporary password for {}: {}",
                email,
                temporaryPassword
        );

        return employeeMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        return employeeMapper.toResponse(
                findActiveEmployee(id)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getAllEmployees(
            Pageable pageable) {

        Page<Employee> page = employeeRepository
                .findAll(pageable);

        return PageResponse.from(
                page.map(employeeMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getEmployeesByRole(
            Role role,
            Pageable pageable) {

        /*
         * Used when PM wants to see all available
         * modelers/editors/checkers for project assignment.
         */
        Page<Employee> page = employeeRepository
                .findAll(pageable);

        return PageResponse.from(
                page.map(employeeMapper::toResponse)
        );
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public EmployeeResponse updateEmployee(
            UUID id,
            UpdateEmployeeRequest request) {

        Employee employee = findActiveEmployee(id);

        /*
         * Only update fields that were actually provided.
         * null = "not provided" = keep existing value.
         * This is the PATCH pattern.
         */
        if (request.getFirstName() != null) {
            employee.setFirstName(
                    request.getFirstName().trim()
            );
        }
        if (request.getLastName() != null) {
            employee.setLastName(
                    request.getLastName().trim()
            );
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            employee.setRole(request.getRole());
        }
        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment());
        }
        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation());
        }
        if (request.getJoiningDate() != null) {
            employee.setJoiningDate(request.getJoiningDate());
        }
        if (request.getProfilePhotoUrl() != null) {
            employee.setProfilePhotoUrl(
                    request.getProfilePhotoUrl()
            );
        }

        Employee saved = employeeRepository.save(employee);

        log.info("Employee updated: {}", saved.getEmployeeCode());

        return employeeMapper.toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // DISABLE / ENABLE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void disableEmployee(UUID id) {
        Employee employee = findActiveEmployee(id);

        if (!employee.isActive()) {
            throw new BusinessException(
                    "Employee is already disabled",
                    "EMPLOYEE_ALREADY_DISABLED"
            );
        }

        employee.setActive(false);
        employeeRepository.save(employee);

        /*
         * Revoke all active sessions immediately.
         * When an account is disabled, the person
         * should not be able to use existing tokens.
         * Their next request will fail at CustomUserDetailsService
         * because isActive = false.
         * But we also revoke refresh tokens so they cannot
         * get new access tokens.
         */
        refreshTokenRepository.revokeAllForEmployee(id);

        log.info(
                "Employee disabled: {}",
                employee.getEmployeeCode()
        );
    }

    @Transactional
    public void enableEmployee(UUID id) {
        /*
         * Use findByIdAndIsDeletedFalse here — not findActiveEmployee
         * because findActiveEmployee filters isActive = true.
         * We need to find disabled employees too.
         */
        Employee employee = employeeRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee", id.toString()
                        )
                );

        if (employee.isActive()) {
            throw new BusinessException(
                    "Employee is already active",
                    "EMPLOYEE_ALREADY_ACTIVE"
            );
        }

        employee.setActive(true);
        employee.setFailedLoginAttempts(0);
        employee.setAccountLockedUntil(null);
        employeeRepository.save(employee);

        log.info(
                "Employee enabled: {}",
                employee.getEmployeeCode()
        );
    }

    // ─────────────────────────────────────────────────────────
    // DELETE (SOFT)
    // ─────────────────────────────────────────────────────────

    @Transactional
    public void deleteEmployee(UUID id, String deletedBy) {
        Employee employee = employeeRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee", id.toString()
                        )
                );

        /*
         * Use BaseEntity's markAsDeleted method.
         * Sets isDeleted = true, deletedAt, deletedBy.
         */
        employee.markAsDeleted(deletedBy);
        employeeRepository.save(employee);

        // Revoke all sessions
        refreshTokenRepository.revokeAllForEmployee(id);

        log.info(
                "Employee soft deleted: {} by: {}",
                employee.getEmployeeCode(),
                deletedBy
        );
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Employee findActiveEmployee(UUID id) {
        return employeeRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee", id.toString()
                        )
                );
    }

    private String generateEmployeeCode() {

        String prefix = "TWC-";
        long count = employeeRepository
                .countByEmployeeCodeStartingWith(prefix);
        return prefix + String.format("%03d", count + 1);
    }

    private String generateTemporaryPassword(
            String firstName) {
        int randomNum = 1234;
        return firstName.substring(0, 1).toUpperCase()
                + firstName.substring(1).toLowerCase()
                + "@" + randomNum + "!";
    }

    // ─────────────────────────────────────────────────────────
// ATTENDANCE PIN MANAGEMENT
// ─────────────────────────────────────────────────────────

    @Transactional
    public void setAttendancePin(
            UUID employeeId,
            AttendancePinRequest request,
            String setByEmail) {

        Employee employee = findActiveEmployee(employeeId);

        /*
         * Hash the PIN exactly like a password.
         * 4 digits is weak by password standards,
         * but hashing protects against database compromise.
         * Even if DB is stolen, PINs cannot be reversed.
         */
        employee.setAttendancePinHash(
                passwordEncoder.encode(request.getPin())
        );
        employee.setAttendancePinSetBy(setByEmail);
        employee.setAttendancePinSetAt(Instant.now());

        employeeRepository.save(employee);

        log.info(
                "Attendance PIN set for employee: {} by: {}",
                employee.getEmployeeCode(),
                setByEmail
        );
    }

    @Transactional
    public void resetAttendancePin(
            UUID employeeId,
            String resetByEmail) {

        Employee employee = findActiveEmployee(employeeId);

        employee.setAttendancePinHash(null);
        employee.setAttendancePinSetBy(null);
        employee.setAttendancePinSetAt(null);

        employeeRepository.save(employee);

        log.info(
                "Attendance PIN reset for employee: {} by: {}",
                employee.getEmployeeCode(),
                resetByEmail
        );
    }
}