package com.trade.app.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object for AI reasoning requests.
 * 
 * This DTO represents the input payload for AI reasoning tasks,
 * containing the user's query and optional configuration parameters.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequestDTO {
    
    /**
     * The main content or question to send to the AI model.
     * This is the primary input for reasoning tasks.
     */
    private String input;
    
    /**
     * Optional system instructions or context for the AI model.
     * Used to guide the model's behavior and response style.
     */
    private String systemInstructions;
    
    /**
     * Maximum number of tokens to generate in the response.
     * If null, uses the model's default.
     */
    private Integer maxTokens;
    
    /**
     * Temperature for response generation (0.0 to 2.0).
     * Lower values make output more focused and deterministic.
     * Higher values increase randomness and creativity.
     */
    private Double temperature;
    
    /**
     * Unique identifier for tracking this request.
     * Useful for logging and correlation.
     */
    private String requestId;
    
    /**
     * Additional metadata to include with the request.
     * Can contain context information, user IDs, or other tracking data.
     */
    private Map<String, Object> metadata;
    
    /**
     * Response format specification.
     * Can specify JSON schema or other structured output requirements.
     */
    private ResponseFormat responseFormat;
    
    /**
     * The unique ID of the previous response to the model.
     * Use this to create multi-turn conversations with conversation state.
     * Cannot be used in conjunction with conversationId.
     */
    private String previousResponseId;
    
    /**
     * Whether to store the generated model response for later retrieval via API.
     * Default: true
     */
    @Builder.Default
    private Boolean store = true;
    
    /**
     * The conversation that this response belongs to.
     * Items from this conversation are prepended to input for this response request.
     * Cannot be used in conjunction with previousResponseId.
     */
    private String conversationId;
    
    /**
     * Whether to run the model response in the background.
     * Use for long-running tasks. Default: false
     */
    @Builder.Default
    private Boolean background = false;
    
    /**
     * Safety identifier for detecting policy violations.
     * Should be a stable identifier that uniquely identifies each user.
     * Recommend hashing username or email to avoid sending PII.
     */
    private String safetyIdentifier;
    
    /**
     * Used by OpenAI to cache responses for similar requests.
     * Helps optimize cache hit rates.
     */
    private String promptCacheKey;
    
    /**
     * Nested class for response format configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        
        /**
         * Type of response format (e.g., "json_object", "text").
         */
        private String type;
        
        /**
         * JSON schema for structured output (if type is json_object).
         */
        private Map<String, Object> jsonSchema;
    }
    
    /**
     * Validates that the request has minimum required fields.
     * 
     * @return true if the request is valid, false otherwise
     */
    public boolean isValid() {
        return input != null && !input.isBlank();
    }
}
