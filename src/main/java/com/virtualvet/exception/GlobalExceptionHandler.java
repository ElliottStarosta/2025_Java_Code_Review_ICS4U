package com.virtualvet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Virtual Vet application.
 * 
 * This class provides centralized exception handling across all REST controllers
 * in the application. It catches various types of exceptions and returns
 * appropriate HTTP responses with standardized error formats. The handler
 * ensures consistent error responses and proper HTTP status codes for different
 * error scenarios.
 * 
 * The handler uses Spring's @RestControllerAdvice annotation to intercept
 * exceptions from all controllers and provides specific handling for file upload
 * errors, argument validation errors, runtime exceptions, and generic exceptions.
 * 
 * @author Elliott Starosta
 * @version 1.0
 * @since 2025
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles file upload size limit exceeded exceptions.
     * 
     * This method catches MaxUploadSizeExceededException when users attempt to
     * upload files that exceed the maximum allowed size (typically 5MB for images).
     * It returns a standardized error response with appropriate HTTP status code
     * and clear messaging about the file size limitation.
     * 
     * @param e the MaxUploadSizeExceededException that was thrown
     * @return ResponseEntity containing error details with HTTP 413 Payload Too Large status
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException e) {
        // Create standardized error response for file size violations
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", "File size exceeds maximum allowed size of 5MB");
        response.put("code", "FILE_TOO_LARGE");
        response.put("retry", false);
        
        // Return HTTP 413 Payload Too Large status with error details
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    /**
     * Handles illegal argument exceptions from parameter validation.
     * 
     * This method catches IllegalArgumentException when invalid parameters are
     * passed to service methods or controllers. It provides a standardized
     * error response that includes the original error message for debugging
     * while maintaining consistent error format across the API.
     * 
     * @param e the IllegalArgumentException that was thrown
     * @return ResponseEntity containing error details with HTTP 400 Bad Request status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        // Create standardized error response for invalid arguments
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", e.getMessage());
        response.put("code", "INVALID_ARGUMENT");
        response.put("retry", false);
        
        // Return HTTP 400 Bad Request status with error details
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles runtime exceptions from service layer operations.
     * 
     * This method catches RuntimeException instances that may occur during
     * business logic execution, such as service failures, data access issues,
     * or other operational problems. It provides a standardized error response
     * with retry indication for transient issues.
     * 
     * @param e the RuntimeException that was thrown
     * @return ResponseEntity containing error details with HTTP 500 Internal Server Error status
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        // Create standardized error response for runtime exceptions
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", "An unexpected error occurred: " + e.getMessage());
        response.put("code", "INTERNAL_ERROR");
        response.put("retry", true);
        
        // Return HTTP 500 Internal Server Error status with error details
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles generic exceptions as a fallback for unhandled exception types.
     * 
     * This method serves as a catch-all for any exceptions not handled by
     * more specific exception handlers. It provides a generic error message
     * to avoid exposing internal system details while maintaining consistent
     * error response format and suggesting retry for potential transient issues.
     * 
     * @param e the Exception that was thrown
     * @return ResponseEntity containing generic error details with HTTP 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        // Create standardized error response for generic exceptions
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", "Service temporarily unavailable");
        response.put("code", "SERVICE_ERROR");
        response.put("retry", true);
        
        // Return HTTP 500 Internal Server Error status with generic error details
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}