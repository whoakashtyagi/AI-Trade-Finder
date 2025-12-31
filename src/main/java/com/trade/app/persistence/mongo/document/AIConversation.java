package com.trade.app.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document for storing AI conversation state.
 * 
 * This enables multi-turn conversations using OpenAI's Response API
 * with previous_response_id for conversation continuity.
 * 
 * Each conversation tracks a series of exchanges with the AI model,
 * maintaining response IDs for context preservation.
 * 
 * @author AI Trade Finder Team
 */
@Document(collection = "ai_conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIConversation {
    
    @Id
    private String id;
    
    /**
     * Unique identifier for the conversation session.
     * Used to group related AI exchanges.
     */
    private String conversationId;
    
    /**
     * Symbol this conversation is about (if applicable).
     */
    private String symbol;
    
    /**
     * Type of conversation (e.g., "TRADE_ANALYSIS", "MARKET_ANALYSIS", "WORKFLOW", "TRADE_FOLLOWUP").
     */
    private String conversationType;
    
    /**
     * User or system identifier who initiated the conversation.
     */
    private String userId;
    
    // ========== Entity References ==========
    
    /**
     * Reference to an identified trade document ID.
     * Used when the conversation is about a specific trade opportunity.
     */
    @Indexed
    @Field("trade_id")
    private String tradeId;
    
    /**
     * Type of entity this conversation is about.
     * Values: "TRADE", "SYMBOL", "PATTERN", "MARKET", "GENERAL"
     */
    @Indexed
    @Field("entity_type")
    private String entityType;
    
    /**
     * Generic entity ID reference for extensibility.
     * Can reference any domain object.
     */
    @Field("entity_id")
    private String entityId;
    
    /**
     * Snapshot of entity data at conversation start.
     * For trades: includes entry, stop, targets, confidence, etc.
     * This preserves context even if the original entity changes.
     */
    @Field("entity_snapshot")
    private Map<String, Object> entitySnapshot;
    
    /**
     * Ordered list of response IDs from OpenAI.
     * Each entry represents one turn in the conversation.
     * The last entry is the most recent response.
     */
    @Builder.Default
    private List<ConversationTurn> turns = new ArrayList<>();
    
    /**
     * Current status of the conversation.
     */
    private String status; // ACTIVE, COMPLETED, EXPIRED
    
    /**
     * Timestamp when conversation was created.
     */
    private Instant createdAt;
    
    /**
     * Timestamp of last activity in this conversation.
     */
    private Instant lastActivityAt;
    
    /**
     * When this conversation expires (for cleanup).
     */
    private Instant expiresAt;
    
    /**
     * Additional metadata about the conversation.
     */
    private Map<String, Object> metadata;
    
    /**
     * Conversation context data that persists across turns.
     * For trades: technical levels, market conditions, user preferences.
     * This is dynamically updated and referenced during the conversation.
     */
    @Field("context_data")
    private Map<String, Object> contextData;
    
    /**
     * User's trading preferences relevant to this conversation.
     * Examples: risk tolerance, preferred timeframes, trading style.
     */
    @Field("user_preferences")
    private Map<String, Object> userPreferences;
    
    /**
     * Tags for categorizing and filtering conversations.
     * Examples: ["high-confidence", "scalp-setup", "news-driven"]
     */
    @Field("tags")
    private List<String> tags;
    
    /**
     * Represents a single turn in the conversation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationTurn {
        
        /**
         * OpenAI Response ID from this turn.
         */
        private String responseId;
        
        /**
         * Request ID that generated this response (for correlation).
         */
        private String requestId;
        
        /**
         * Summary of the user's input in this turn.
         */
        private String userInputSummary;
        
        /**
         * Summary of the AI's output in this turn.
         */
        private String aiOutputSummary;
        
        /**
         * Model used for this turn.
         */
        private String model;
        
        /**
         * Timestamp of this turn.
         */
        private Instant timestamp;
        
        /**
         * Tokens used in this turn.
         */
        private Integer tokensUsed;
        
        /**
         * Turn-specific metadata.
         */
        private Map<String, Object> metadata;
    }
    
    /**
     * Gets the most recent response ID (for use as previousResponseId).
     * 
     * @return The last response ID, or null if no turns exist
     */
    public String getLatestResponseId() {
        if (turns == null || turns.isEmpty()) {
            return null;
        }
        return turns.get(turns.size() - 1).getResponseId();
    }
    
    /**
     * Adds a new turn to the conversation.
     * 
     * @param turn The conversation turn to add
     */
    public void addTurn(ConversationTurn turn) {
        if (turns == null) {
            turns = new ArrayList<>();
        }
        turns.add(turn);
        this.lastActivityAt = Instant.now();
    }
}
