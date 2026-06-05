package com.thewizecompany.wizevision.shared.exception;

/*
 * Thrown when a requested entity does not exist.
 *
 * Usage in service:
 *   Employee employee = employeeRepository
 *       .findByIdAndIsDeletedFalse(id)
 *       .orElseThrow(() -> new ResourceNotFoundException(
 *           "Employee", id.toString()
 *       ));
 *
 * Results in:
 * HTTP 404
 * {
 *   "success": false,
 *   "message": "Employee not found with id: uuid-here",
 *   "errorCode": "RESOURCE_NOT_FOUND"
 * }
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(
            String resourceName,
            String identifier) {
        super(resourceName + " not found with id: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}