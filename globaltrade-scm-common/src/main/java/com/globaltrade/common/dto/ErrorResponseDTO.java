package com.globaltrade.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Standardized error response DTO for REST API error handling.
 *
 * <p><strong>Enterprise Exception Handling Pattern:</strong></p>
 * <ul>
 *   <li><strong>Consistent Structure:</strong> All API errors follow this format
 *       for predictable client handling</li>
 *   <li><strong>Security:</strong> No stack traces or sensitive information exposed</li>
 *   <li><strong>Debugging:</strong> Timestamp and correlation ID support troubleshooting</li>
 *   <li><strong>Validation:</strong> Support for field-level validation errors</li>
 * </ul>
 */
public class ErrorResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String correlationId;
    private List<FieldError> fieldErrors;

    /**
     * Default constructor for frameworks that require no-arg constructors.
     */
    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now().toString();
        this.fieldErrors = new ArrayList<>();
    }

    /**
     * Constructor for basic error responses.
     */
    public ErrorResponseDTO(int status, String error, String message) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    /**
     * Constructor with path information.
     */
    public ErrorResponseDTO(int status, String error, String message, String path) {
        this(status, error, message);
        this.path = path;
    }

    /**
     * Constructor with field validation errors.
     */
    public ErrorResponseDTO(int status, String error, String message, List<ErrorResponseDTO.FieldError> fieldErrors) {
        this(status, error, message);
        this.fieldErrors = fieldErrors;
    }

    /**
     * Constructor with complete information.
     */
    public ErrorResponseDTO(int status, String error, String message, String path,
                            String correlationId, List<ErrorResponseDTO.FieldError> fieldErrors) {
        this(status, error, message, path);
        this.correlationId = correlationId;
        this.fieldErrors = fieldErrors;
    }

    // Static factory methods for common error types

    /**
     * Creates a 400 Bad Request error response.
     */
    public static ErrorResponseDTO badRequest(String message) {
        return new ErrorResponseDTO(400, "Bad Request", message);
    }

    /**
     * Creates a 404 Not Found error response.
     */
    public static ErrorResponseDTO notFound(String message) {
        return new ErrorResponseDTO(404, "Not Found", message);
    }

    /**
     * Creates a 409 Conflict error response.
     */
    public static ErrorResponseDTO conflict(String message) {
        return new ErrorResponseDTO(409, "Conflict", message);
    }

    /**
     * Creates a 422 Unprocessable Entity error response (for validation failures).
     */
    public static ErrorResponseDTO unprocessableEntity(String message, List<ErrorResponseDTO.FieldError> fieldErrors) {
        return new ErrorResponseDTO(422, "Unprocessable Entity", message, fieldErrors);
    }

    /**
     * Creates a 500 Internal Server Error response.
     */
    public static ErrorResponseDTO internalServerError(String message) {
        return new ErrorResponseDTO(500, "Internal Server Error", message);
    }

    /**
     * Creates a 503 Service Unavailable response.
     */
    public static ErrorResponseDTO serviceUnavailable(String message) {
        return new ErrorResponseDTO(503, "Service Unavailable", message);
    }

    // Inner class for field-level validation errors
    public static class FieldError implements Serializable {
        private static final long serialVersionUID = 1L;

        private String field;
        private String rejectedValue;
        private String message;

        public FieldError() {
        }

        public FieldError(String field, String rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        // Getters and Setters
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(String rejectedValue) {
            this.rejectedValue = rejectedValue;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "FieldError{" +
                    "field='" + field + '\'' +
                    ", rejectedValue='" + rejectedValue + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    // Getters and Setters
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<ErrorResponseDTO.FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<ErrorResponseDTO.FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public void addFieldError(String field, String rejectedValue, String message) {
        this.fieldErrors.add(new FieldError(field, rejectedValue, message));
    }

    @Override
    public String toString() {
        return "ErrorResponseDTO{" +
                "timestamp='" + timestamp + '\'' +
                ", status=" + status +
                ", error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", fieldErrors=" + fieldErrors +
                '}';
    }
}