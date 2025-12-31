package com.trade.app.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response structure for all API errors.
 * 
 * Provides consistent error information to API clients, including
 * timestamp, status code, error type, message, and optional details.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;
    
    /**
     * HTTP status code.
     */
    private Integer status;
    
    /**
     * Error type or category.
     */
    private String error;
    
    /**
     * Human-readable error message.
     */
    private String message;
    
    /**
     * Request path that caused the error.
     */
    private String path;
    
    /**
     * Optional request ID for correlation.
     */
    private String requestId;
    
    /**
     * Optional additional error details.
     */
    private Map<String, Object> details;
}
