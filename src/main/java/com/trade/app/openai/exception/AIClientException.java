package com.trade.app.openai.exception;

/**
 * Base exception for AI client operations.
 * 
 * This exception is thrown when errors occur during AI service interactions,
 * including network errors, API errors, timeout issues, or invalid responses.
 * 
 * @author AI Trade Finder Team
 */
public class AIClientException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * HTTP status code if the error was from an API response.
     */
    private final Integer statusCode;
    
    /**
     * Error code from the AI service provider (if available).
     */
    private final String errorCode;
    
    /**
     * Request ID for correlation and debugging.
     */
    private final String requestId;
    
    /**
     * Constructs a new AIClientException with a message.
     * 
     * @param message The error message
     */
    public AIClientException(String message) {
        super(message);
        this.statusCode = null;
        this.errorCode = null;
        this.requestId = null;
    }
    
    /**
     * Constructs a new AIClientException with a message and cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public AIClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.errorCode = null;
        this.requestId = null;
    }
    
    /**
     * Constructs a new AIClientException with detailed error information.
     * 
     * @param message The error message
     * @param statusCode HTTP status code
     * @param errorCode Error code from the provider
     * @param requestId Request ID for correlation
     */
    public AIClientException(String message, Integer statusCode, String errorCode, String requestId) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.requestId = requestId;
    }
    
    /**
     * Constructs a new AIClientException with detailed error information and cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     * @param statusCode HTTP status code
     * @param errorCode Error code from the provider
     * @param requestId Request ID for correlation
     */
    public AIClientException(String message, Throwable cause, Integer statusCode, String errorCode, String requestId) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.requestId = requestId;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * Checks if this exception represents a retryable error.
     * 
     * Retryable errors include:
     * - Network timeouts
     * - 5xx server errors
     * - 429 rate limit errors
     * 
     * @return true if the error is potentially retryable
     */
    public boolean isRetryable() {
        if (statusCode == null) {
            return false;
        }
        // Retry on server errors (5xx) or rate limits (429)
        return statusCode >= 500 || statusCode == 429;
    }
}
