package com.trade.app.openai.dto;

import com.trade.app.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object for AI reasoning responses.
 * 
 * This DTO represents the structured output received from the AI service,
 * including the generated content, metadata, and usage statistics.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponseDTO {
    
    /**
     * Unique identifier for this response from the AI service.
     */
    private String id;
    
    /**
     * Model used to generate this response (e.g., "o1", "o1-preview").
     */
    private String model;
    
    /**
     * The generated output content from the AI model.
     * This is the main reasoning result.
     */
    private String output;
    
    /**
     * Timestamp when the response was created.
     */
    private Instant createdAt;
    
    /**
     * Status of the response (e.g., "completed", "failed", "in_progress").
     */
    private String status;
    
    /**
     * Finish reason indicating why the generation stopped.
     * Examples: "stop", "length", "content_filter".
     */
    private String finishReason;
    
    /**
     * Token usage statistics for this request/response.
     */
    private Usage usage;
    
    /**
     * Additional metadata returned by the AI service.
     */
    private Map<String, Object> metadata;
    
    /**
     * Optional structured data if JSON output was requested.
     */
    private Map<String, Object> structuredOutput;
    
    /**
     * Request ID that corresponds to this response (for correlation).
     */
    private String requestId;
    
    /**
     * Processing time in milliseconds.
     */
    private Long processingTimeMs;
    
    /**
     * The previous response ID if this was part of a conversation chain.
     */
    private String previousResponseId;
    
    /**
     * The conversation ID this response belongs to.
     */
    private String conversationId;
    
    /**
     * Whether this response was stored for future retrieval.
     */
    private Boolean stored;
    
    /**
     * Object type from the API (should be "response").
     */
    private String object;
    
    /**
     * Service tier used to process the request (default, flex, priority).
     */
    private String serviceTier;
    
    /**
     * Error details if the response failed.
     */
    private ErrorDetails error;
    
    /**
     * Nested class for error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetails {
        private String message;
        private String type;
        private String code;
        private Map<String, Object> details;
    }
    
    /**
     * Nested class for token usage statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        
        /**
         * Number of tokens in the prompt/input.
         */
        private Integer promptTokens;
        
        /**
         * Number of tokens in the completion/output.
         */
        private Integer completionTokens;
        
        /**
         * Total tokens used (prompt + completion).
         */
        private Integer totalTokens;
        
        /**
         * Number of tokens used for reasoning (if applicable).
         */
        private Integer reasoningTokens;
    }
    
    /**
     * Checks if the response was successful.
     * 
     * @return true if status is "completed" and output is present
     */
    public boolean isSuccessful() {
        return Constants.AIResponseStatus.COMPLETED.equalsIgnoreCase(status) && output != null && !output.isBlank();
    }
}
