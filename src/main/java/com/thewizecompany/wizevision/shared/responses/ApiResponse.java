package com.thewizecompany.wizevision.shared.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/*
 * STANDARD API RESPONSE WRAPPER
 *
 * Every single endpoint in this system returns this structure.
 * No exceptions.
 *
 * Why a wrapper?
 * Without wrapper — frontend gets inconsistent responses:
 *   Success: { "id": "123", "name": "ABC" }
 *   Error:   { "timestamp": "...", "status": 400, "error": "..." }
 *   These are completely different shapes — frontend must handle both
 *
 * With wrapper — frontend always gets the same shape:
 *   Success: { "success": true,  "message": "...", "data": {...} }
 *   Error:   { "success": false, "message": "...", "data": null }
 *   Frontend always reads .success then .data — simple and consistent
 *
 * @JsonInclude(NON_NULL) means null fields are omitted from JSON.
 * errorCode is null on success → not sent to frontend.
 * data is null on error → not sent to frontend.
 * Keeps responses clean.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String errorCode;
    private final String correlationId;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    /*
     * STATIC FACTORY METHODS
     * These are convenience methods so you never
     * construct ApiResponse manually in controllers.
     *
     * Usage in controller:
     *   return ResponseEntity.ok(ApiResponse.ok(employeeResponse));
     *   return ResponseEntity.ok(ApiResponse.ok(employeeResponse, "Employee created"));
     *   return ResponseEntity.badRequest().body(ApiResponse.error("Not found", "EMP_001"));
     */

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }

    public static <T> ApiResponse<T> error(
            String message,
            String errorCode,
            String correlationId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .correlationId(correlationId)
                .build();
    }
}