package com.trade.app.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for conversation operations.
 * 
 * Provides a clean API response for conversation creation and queries,
 * suitable for REST endpoints and UI consumption.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDTO {
    
    /**
     * The conversation ID for follow-up interactions.
     */
    private String conversationId;
    
    /**
     * Type of conversation.
     */
    private String conversationType;
    
    /**
     * Entity reference information.
     */
    private EntityReference entityReference;
    
    /**
     * Current conversation status.
     */
    private String status;
    
    /**
     * Number of turns in this conversation.
     */
    private Integer turnCount;
    
    /**
     * When the conversation was created.
     */
    private Instant createdAt;
    
    /**
     * Last activity timestamp.
     */
    private Instant lastActivityAt;
    
    /**
     * When the conversation expires.
     */
    private Instant expiresAt;
    
    /**
     * Tags associated with this conversation.
     */
    private List<String> tags;
    
    /**
     * Summary of the trade or entity being discussed.
     */
    private String summary;
    
    /**
     * Suggested follow-up questions the trader might ask.
     */
    private List<String> suggestedQuestions;
    
    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;
    
    /**
     * Entity reference details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityReference {
        private String entityType;
        private String entityId;
        private String symbol;
        private Map<String, Object> snapshot;
    }
}
