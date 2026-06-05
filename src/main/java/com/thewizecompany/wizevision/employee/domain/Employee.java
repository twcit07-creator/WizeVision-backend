package com.thewizecompany.wizevision.employee.domain;

import com.thewizecompany.wizevision.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "employees",
        indexes = {
                @Index(
                        name = "idx_employees_email",
                        columnList = "email",
                        unique = true
                ),
                @Index(
                        name = "idx_employees_code",
                        columnList = "employee_code",
                        unique = true
                ),
                @Index(
                        name = "idx_employees_role",
                        columnList = "role"
                ),
                @Index(
                        name = "idx_employees_department",
                        columnList = "department_id"
                ),
                @Index(
                        name = "idx_employees_is_active",
                        columnList = "is_active"
                ),
                @Index(
                        name = "idx_employees_is_deleted",
                        columnList = "is_deleted"
                ),
                @Index(
                        name = "idx_employees_reporting_manager",
                        columnList = "reporting_manager_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseEntity {

    // ═══════════════════════════════════════════════════
    // PERSONAL INFORMATION
    // ═══════════════════════════════════════════════════

    @Column(
            name = "employee_code",
            nullable = false,
            unique = true,
            length = 20
    )
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "blood_group", length = 5)
    private String bloodGroup;

    /*
     * Two email fields:
     * email         = company email, used for portal login
     * personal_email = personal email, used for payslips
     *                  and emergency communications
     */
    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 255
    )
    private String email;

    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "current_address", length = 500)
    private String currentAddress;

    @Column(name = "permanent_address", length = 500)
    private String permanentAddress;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    // ═══════════════════════════════════════════════════
    // EMERGENCY CONTACT
    // ═══════════════════════════════════════════════════

    @Column(
            name = "emergency_contact_name",
            length = 200
    )
    private String emergencyContactName;

    @Column(
            name = "emergency_contact_phone",
            length = 20
    )
    private String emergencyContactPhone;

    @Column(
            name = "emergency_contact_relation",
            length = 50
    )
    private String emergencyContactRelation;

    // ═══════════════════════════════════════════════════
    // EMPLOYMENT DETAILS
    // ═══════════════════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(
            name = "employment_type",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 30
    )
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation designation;

    /*
     * Self-referencing relationship.
     * An employee's reporting manager is also an employee.
     * FetchType.LAZY is critical here — without it,
     * loading one employee loads their manager,
     * who loads their manager, recursively.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_manager_id")
    private Employee reportingManager;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;

    @Column(name = "notice_period_days")
    @Builder.Default
    private Integer noticePeriodDays = 30;

    @Column(name = "relieving_date")
    private LocalDate relievingDate;

    /*
     * Contract dates — only relevant for
     * CONTRACT and FREELANCE employees.
     */
    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    // ═══════════════════════════════════════════════════
    // PAYROLL INFORMATION
    // ═══════════════════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_structure", length = 20)
    @Builder.Default
    private SalaryStructure salaryStructure =
            SalaryStructure.MONTHLY;

    /*
     * Basic salary:
     * MONTHLY employees  → amount per month
     * DAILY employees    → amount per day
     * HOURLY employees   → amount per hour
     */
    @Column(
            name = "basic_salary",
            precision = 12,
            scale = 2
    )
    private BigDecimal basicSalary;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "pf_number", length = 30)
    private String pfNumber;

    @Column(name = "esi_number", length = 30)
    private String esiNumber;

    @Column(name = "pf_applicable", nullable = false)
    @Builder.Default
    private boolean pfApplicable = false;

    @Column(name = "esi_applicable", nullable = false)
    @Builder.Default
    private boolean esiApplicable = false;

    // ═══════════════════════════════════════════════════
    // IDENTITY DOCUMENTS
    // ═══════════════════════════════════════════════════

    @Column(name = "national_id_number", length = 30)
    private String nationalIdNumber;

    @Column(name = "passport_number", length = 20)
    private String passportNumber;

    @Column(name = "passport_expiry")
    private LocalDate passportExpiry;

    @Column(name = "driving_license", length = 30)
    private String drivingLicense;

    // ═══════════════════════════════════════════════════
    // ATTENDANCE — WINDOWS APP
    // ═══════════════════════════════════════════════════

    /*
     * 4-digit PIN for Windows attendance application.
     * Stored as BCrypt hash — same security as password.
     * HR assigns the PIN, employee cannot change it themselves.
     * HR can reset/change the PIN.
     */
    @Column(name = "attendance_pin_hash", length = 255)
    private String attendancePinHash;

    @Column(name = "attendance_pin_set_by", length = 100)
    private String attendancePinSetBy;

    @Column(name = "attendance_pin_set_at")
    private Instant attendancePinSetAt;

    @Column(name = "default_shift", length = 50)
    private String defaultShift;

    // ═══════════════════════════════════════════════════
    // SECURITY / PORTAL AUTH
    // ═══════════════════════════════════════════════════

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(
            name = "failed_login_attempts",
            nullable = false
    )
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @Column(
            name = "must_change_password",
            nullable = false
    )
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    // ═══════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ═══════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════

    public String getFullName() {
        if (middleName != null && !middleName.isBlank()) {
            return firstName + " " + middleName + " " + lastName;
        }
        return firstName + " " + lastName;
    }

    public boolean isAccountLocked() {
        if (accountLockedUntil == null) return false;
        return Instant.now().isBefore(accountLockedUntil);
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.accountLockedUntil =
                    Instant.now().plusSeconds(1800);
        }
    }

    public void recordSuccessfulLogin(String ipAddress) {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ipAddress;
    }

    public boolean hasAttendancePin() {
        return attendancePinHash != null
                && !attendancePinHash.isBlank();
    }

    public boolean isContractExpired() {
        if (contractEndDate == null) return false;
        return LocalDate.now().isAfter(contractEndDate);
    }
}