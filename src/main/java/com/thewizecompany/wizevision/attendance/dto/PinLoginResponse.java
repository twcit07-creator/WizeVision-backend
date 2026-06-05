package com.thewizecompany.wizevision.attendance.dto;

import com.thewizecompany.wizevision.employee.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/*
 * Returned to Windows app after successful PIN login.
 *
 * The Windows app uses:
 * - employeeId to tag all subsequent events
 * - sessionToken for authenticated requests
 * - fullName to display on the attendance screen
 * - role to show/hide features in the Windows app
 */
@Getter
@Builder
public class PinLoginResponse {

    private final UUID employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String email;
    private final Role role;
    private final String department;

    /*
     * Short-lived token for the Windows app session.
     * Windows app includes this in all subsequent requests.
     * This is a separate token from the portal JWT.
     * Expires after 12 hours (one work shift).
     */
    private final String sessionToken;
    private final long sessionExpiresInMs;
}