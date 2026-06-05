package com.thewizecompany.wizevision.shared.exception;

import com.thewizecompany.wizevision.shared.responses.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/*
 * GLOBAL EXCEPTION HANDLER
 *
 * Catches every exception thrown anywhere in the application
 * and converts it into a consistent ApiResponse JSON.
 *
 * WITHOUT THIS:
 * - Validation errors return Spring's ugly default format
 * - 404s return HTML or inconsistent JSON
 * - Unhandled exceptions expose stack traces to clients
 * - Each error looks different — frontend cannot parse reliably
 *
 * WITH THIS:
 * - Every error returns ApiResponse format
 * - No stack traces exposed to clients
 * - Errors are logged server-side with full details
 * - Frontend always knows what to expect
 *
 * @RestControllerAdvice = applies to ALL controllers
 * @ExceptionHandler = handles specific exception types
 *
 * ORDER MATTERS: More specific exceptions must be
 * listed before more general ones. If RuntimeException
 * handler is first, it catches everything before
 * more specific handlers run.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────────────────
    // VALIDATION ERRORS — 400 Bad Request
    // ─────────────────────────────────────────────────────────

    /*
     * Handles @Valid failures on @RequestBody.
     * Returns a map of field → error message so frontend
     * can highlight specific form fields.
     *
     * Example response:
     * {
     *   "success": false,
     *   "message": "Validation failed",
     *   "errorCode": "VALIDATION_ERROR",
     *   "data": {
     *     "email": "Please provide a valid email address",
     *     "password": "Password must be between 8 and 128 characters"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>>
    handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String fieldName = error instanceof FieldError
                            ? ((FieldError) error).getField()
                            : error.getObjectName();
                    String message = error.getDefaultMessage();
                    errors.put(fieldName, message);
                });

        log.debug("Validation failed: {}", errors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .errorCode("VALIDATION_ERROR")
                        .data(errors)
                        .build()
                );
    }

    /*
     * Handles @Validated failures on path/query parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>>
    handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> errors = new HashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            errors.put(field, violation.getMessage());
        }

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .errorCode("VALIDATION_ERROR")
                        .data(errors)
                        .build()
                );
    }

    // ─────────────────────────────────────────────────────────
    // AUTHENTICATION ERRORS — 401 Unauthorized
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleBadCredentials(BadCredentialsException ex) {

        log.debug("Bad credentials: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        "INVALID_CREDENTIALS"
                ));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleLocked(LockedException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        "ACCOUNT_LOCKED"
                ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleDisabled(DisabledException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        "ACCOUNT_DISABLED"
                ));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleExpiredJwt(ExpiredJwtException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        "Your session has expired. Please login again.",
                        "TOKEN_EXPIRED"
                ));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleJwtException(JwtException ex) {

        log.warn("JWT error: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        "Invalid authentication token.",
                        "INVALID_TOKEN"
                ));
    }

    // ─────────────────────────────────────────────────────────
    // AUTHORIZATION ERRORS — 403 Forbidden
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleAccessDenied(AccessDeniedException ex) {

        log.debug("Access denied: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "You do not have permission to access this resource.",
                        "ACCESS_DENIED"
                ));
    }

    // ─────────────────────────────────────────────────────────
    // NOT FOUND — 404
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        log.debug("Resource not found: {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        "The requested endpoint does not exist: "
                                + request.getRequestURI(),
                        "ENDPOINT_NOT_FOUND"
                ));
    }

    /*
     * Handles our own ResourceNotFoundException
     * thrown from service layer when entity is not found.
     * Example: employeeService.getById(unknownId)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.debug("Entity not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        "RESOURCE_NOT_FOUND"
                ));
    }

    // ─────────────────────────────────────────────────────────
    // BUSINESS LOGIC ERRORS — 409 Conflict / 422 Unprocessable
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleDuplicate(DuplicateResourceException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        "DUPLICATE_RESOURCE"
                ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleBusiness(BusinessException ex) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        ex.getErrorCode()
                ));
    }

    // ─────────────────────────────────────────────────────────
    // MISSING HEADERS — 400
    // ─────────────────────────────────────────────────────────

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleMissingHeader(MissingRequestHeaderException ex) {

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        "Required header missing: " + ex.getHeaderName(),
                        "MISSING_HEADER"
                ));
    }

    // ─────────────────────────────────────────────────────────
    // CATCH-ALL — 500 Internal Server Error
    // ─────────────────────────────────────────────────────────

    /*
     * This catches anything not handled above.
     *
     * CRITICAL: We log the FULL exception here server-side
     * but only return a generic message to the client.
     * Never expose stack traces or internal error details
     * to API consumers — that is an information leak.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>>
    handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Unhandled exception for {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage(),
                ex  // full stack trace in logs only
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. "
                                + "Please try again or contact support.",
                        "INTERNAL_ERROR"
                ));
    }

    @ExceptionHandler(OverBillingException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleOverBilling(OverBillingException ex) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        "OVER_BILLING"
                ));
    }
}