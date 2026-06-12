package com.thewizecompany.wizevision.employee.repository;

import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository
        extends JpaRepository<Employee, UUID> {

    /*
     * PRIMARY LOGIN QUERY
     *
     * Finds active, non-deleted employee by email.
     * Used by CustomUserDetailsService for authentication.
     *
     * Why filter isDeleted and isActive here?
     * A deleted or inactive employee must not be able to login.
     * Filtering at the query level is safer than
     * checking in service code — one fewer place to forget.
     */
    Optional<Employee> findByEmailAndIsDeletedFalseAndIsActiveTrue(
            String email
    );

    /*
     * Used for duplicate email check during employee creation.
     * Checks ALL employees including deleted ones —
     * we do not want to reuse an email that belonged
     * to a deleted account.
     */
    boolean existsByEmail(String email);

    /*
     * Used for duplicate code check during employee creation.
     */
    boolean existsByEmployeeCode(String employeeCode);

    /*
     * Finds any employee by ID that is not deleted.
     * Used for profile lookups, assignment lookups, etc.
     * Inactive employees can still be looked up
     * (admins need to see their records).
     */
    Optional<Employee> findByIdAndIsDeletedFalse(UUID id);

    /*
     * COUNT QUERY — used for generating next employee code.
     * WV-2025-001, WV-2025-002, etc.
     * We count total employees (including deleted and inactive)
     * to ensure codes are never reused.
     */
    long countByEmployeeCodeStartingWith(String prefix);

    /*
     * Used by admin to find employees by role.
     * Example: find all available modelers for project assignment.
     */
    java.util.List<Employee> findByRoleAndIsActiveTrue(Role role);

    /*
     * Updates last login details directly in database.
     * More efficient than loading the full entity,
     * setting fields, and saving.
     *
     * @Modifying = tells Spring this is an UPDATE/DELETE query
     * clearAutomatically = clears persistence context cache
     * after update so subsequent reads see fresh data
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Employee e
        SET e.lastLoginAt = CURRENT_TIMESTAMP,
            e.lastLoginIp = :ip,
            e.failedLoginAttempts = 0,
            e.accountLockedUntil = NULL
        WHERE e.id = :id
        """)
    void updateLoginSuccess(
            @Param("id") UUID id,
            @Param("ip") String ip
    );

    List<Employee> findAllByRole(Role role);
}