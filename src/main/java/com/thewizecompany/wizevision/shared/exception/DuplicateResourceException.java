package com.thewizecompany.wizevision.shared.exception;

/*
 * Thrown when trying to create a resource that already exists.
 *
 * Usage in service:
 *   if (employeeRepository.existsByEmail(email)) {
 *       throw new DuplicateResourceException(
 *           "Employee", "email", email
 *       );
 *   }
 *
 * Results in:
 * HTTP 409 Conflict
 * {
 *   "success": false,
 *   "message": "Employee already exists with email: john@test.com",
 *   "errorCode": "DUPLICATE_RESOURCE"
 * }
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(
            String resourceName,
            String fieldName,
            String fieldValue) {
        super(resourceName + " already exists with "
                + fieldName + ": " + fieldValue);
    }
}