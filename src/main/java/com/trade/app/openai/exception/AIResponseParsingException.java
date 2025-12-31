package com.trade.app.openai.exception;

/**
 * Exception thrown when AI response parsing or processing fails.
 * 
 * This exception indicates that a response was received from the AI service
 * but could not be properly parsed or mapped to the expected format.
 * 
 * @author AI Trade Finder Team
 */
public class AIResponseParsingException extends AIClientException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * The raw response that failed to parse.
     */
    private final String rawResponse;
    
    /**
     * Constructs a new AIResponseParsingException with a message.
     * 
     * @param message The parsing error message
     */
    public AIResponseParsingException(String message) {
        super("Response parsing failed: " + message);
        this.rawResponse = null;
    }
    
    /**
     * Constructs a new AIResponseParsingException with a message and cause.
     * 
     * @param message The parsing error message
     * @param cause The underlying cause
     */
    public AIResponseParsingException(String message, Throwable cause) {
        super("Response parsing failed: " + message, cause);
        this.rawResponse = null;
    }
    
    /**
     * Constructs a new AIResponseParsingException with raw response data.
     * 
     * @param message The parsing error message
     * @param rawResponse The raw response that failed to parse
     */
    public AIResponseParsingException(String message, String rawResponse) {
        super("Response parsing failed: " + message);
        this.rawResponse = rawResponse;
    }
    
    /**
     * Constructs a new AIResponseParsingException with raw response data and cause.
     * 
     * @param message The parsing error message
     * @param cause The underlying cause
     * @param rawResponse The raw response that failed to parse
     */
    public AIResponseParsingException(String message, Throwable cause, String rawResponse) {
        super("Response parsing failed: " + message, cause);
        this.rawResponse = rawResponse;
    }
    
    public String getRawResponse() {
        return rawResponse;
    }
}
