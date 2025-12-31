package com.trade.app.exception;

import com.trade.app.openai.exception.AIClientException;
import com.trade.app.openai.exception.AIRequestValidationException;
import com.trade.app.openai.exception.AIResponseParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 * 
 * Provides consistent error responses across the application and ensures
 * that exceptions are properly logged and transformed into meaningful
 * HTTP responses.
 * 
 * @author AI Trade Finder Team
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles AI client exceptions (communication errors, API failures).
     */
    @ExceptionHandler(AIClientException.class)
    public ResponseEntity<ErrorResponse> handleAIClientException(
            AIClientException ex, WebRequest request) {
        
        log.error("AI Client Exception: {} (RequestId: {})", 
                ex.getMessage(), ex.getRequestId(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error("AI Service Error")
                .message(ex.getMessage())
                .path(getRequestPath(request))
                .requestId(ex.getRequestId())
                .details(buildAIExceptionDetails(ex))
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handles AI request validation exceptions (invalid input).
     */
    @ExceptionHandler(AIRequestValidationException.class)
    public ResponseEntity<ErrorResponse> handleAIRequestValidationException(
            AIRequestValidationException ex, WebRequest request) {
        
        log.warn("AI Request Validation Exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(ex.getMessage())
                .path(getRequestPath(request))
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles AI response parsing exceptions (malformed AI responses).
     */
    @ExceptionHandler(AIResponseParsingException.class)
    public ResponseEntity<ErrorResponse> handleAIResponseParsingException(
            AIResponseParsingException ex, WebRequest request) {
        
        log.error("AI Response Parsing Exception: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Response Parsing Error")
                .message("Failed to parse AI response: " + ex.getMessage())
                .path(getRequestPath(request))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException (invalid method arguments).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Illegal Argument Exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Argument")
                .message(ex.getMessage())
                .path(getRequestPath(request))
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles IllegalStateException (invalid application state).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        log.error("Illegal State Exception: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal State Error")
                .message(ex.getMessage())
                .path(getRequestPath(request))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles all other uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(getRequestPath(request))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Extracts the request path from the web request.
     */
    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    /**
     * Builds detailed information for AI client exceptions.
     */
    private Map<String, Object> buildAIExceptionDetails(AIClientException ex) {
        Map<String, Object> details = new HashMap<>();
        
        if (ex.getStatusCode() != null) {
            details.put("statusCode", ex.getStatusCode());
        }
        
        if (ex.getErrorCode() != null) {
            details.put("errorCode", ex.getErrorCode());
        }
        
        details.put("retryable", ex.isRetryable());
        
        return details;
    }
}
