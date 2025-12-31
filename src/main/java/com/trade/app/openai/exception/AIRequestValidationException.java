package com.trade.app.openai.exception;

/**
 * Exception thrown when AI request validation fails.
 * 
 * This exception indicates that the request data is invalid or incomplete
 * before being sent to the AI service.
 * 
 * @author AI Trade Finder Team
 */
public class AIRequestValidationException extends AIClientException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new AIRequestValidationException with a message.
     * 
     * @param message The validation error message
     */
    public AIRequestValidationException(String message) {
        super("Request validation failed: " + message);
    }
    
    /**
     * Constructs a new AIRequestValidationException with a message and cause.
     * 
     * @param message The validation error message
     * @param cause The underlying cause
     */
    public AIRequestValidationException(String message, Throwable cause) {
        super("Request validation failed: " + message, cause);
    }
}
