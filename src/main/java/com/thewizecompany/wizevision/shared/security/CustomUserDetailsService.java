package com.thewizecompany.wizevision.shared.security;

import com.thewizecompany.wizevision.employee.domain.Employee;
import com.thewizecompany.wizevision.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * CUSTOM USER DETAILS SERVICE
 *
 * Spring Security calls this during authentication to load
 * user details by username (in our case, email).
 *
 * WHY DO WE NEED THIS?
 *
 * Spring's AuthenticationManager needs to:
 * 1. Load the user by their identifier (email)
 * 2. Compare the provided password against stored hash
 * 3. Check if account is enabled/locked
 *
 * We implement UserDetailsService to tell Spring
 * "use our Employee table, not an in-memory user store"
 *
 * WHAT HAPPENS DURING LOGIN:
 * 1. AuthService calls authenticationManager.authenticate()
 * 2. AuthenticationManager calls loadUserByUsername(email)
 * 3. This method loads the Employee from database
 * 4. Returns UserDetails with hashed password
 * 5. Spring compares provided password with hash
 * 6. If match → authentication succeeds
 * 7. AuthService then generates JWT tokens
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Employee employee = employeeRepository
                .findByEmailAndIsDeletedFalseAndIsActiveTrue(
                        email.toLowerCase().trim()
                )
                .orElseThrow(() -> {
                    /*
                     * SECURITY NOTE:
                     * We log at DEBUG not WARN here.
                     * Logging failed lookups at WARN would let
                     * an attacker enumerate valid emails by
                     * watching server logs.
                     * At DEBUG, it only appears in dev environment.
                     */
                    log.debug(
                            "Employee not found or inactive: {}",
                            email
                    );
                    return new UsernameNotFoundException(
                            "Invalid credentials"
                    );
                });

        /*
         * Check account lock BEFORE returning UserDetails.
         * If we return locked account details, Spring will
         * still try to match the password, consuming resources.
         * Better to fail fast here.
         */
        if (employee.isAccountLocked()) {
            log.warn(
                    "Login attempt on locked account: {}",
                    email
            );
            throw new UsernameNotFoundException(
                    "Account is temporarily locked"
            );
        }

        /*
         * Build Spring Security's UserDetails object.
         *
         * We use email as the username (principal).
         * This is what gets stored in SecurityContext
         * and what AuditorAware reads for createdBy/updatedBy.
         *
         * ROLE_ prefix is required by Spring Security
         * for hasRole() checks to work correctly.
         */
        return User.builder()
                .username(employee.getEmail())
                .password(employee.getPasswordHash())
                .authorities(List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + employee.getRole().name()
                        )
                ))
                .accountLocked(employee.isAccountLocked())
                .disabled(!employee.isActive())
                .build();
    }
}