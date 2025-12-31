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

/**
 * MongoDB document for AI chat sessions.
 * 
 * Enables multi-turn conversations with AI about trades,
 * market analysis, and trading strategies.
 * 
 * @author AI Trade Finder Team
 */
@Document("chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    private String id;

    /**
     * Session title/name.
     */
    @Field("title")
    private String title;

    /**
     * Session type (e.g., "TRADE_DISCUSSION", "MARKET_ANALYSIS", "GENERAL").
     */
    @Indexed
    @Field("type")
    private String type;

    /**
     * Reference to identified trade ID (if discussing a specific trade).
     */
    @Indexed
    @Field("trade_id")
    private String tradeId;

    /**
     * Symbol being discussed (if applicable).
     */
    @Indexed
    @Field("symbol")
    private String symbol;

    /**
     * Chat message history.
     */
    @Field("messages")
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * Session context/metadata for AI continuity.
     */
    @Field("context")
    private String context;

    /**
     * Active/closed status.
     */
    @Field("active")
    private boolean active;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt;

    @Field("created_by")
    private String createdBy;

    /**
     * Individual chat message within a session.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        
        /**
         * Message role: "user" or "assistant".
         */
        private String role;

        /**
         * Message content.
         */
        private String content;

        /**
         * Timestamp of the message.
         */
        @Builder.Default
        private Instant timestamp = Instant.now();

        /**
         * Token count (for tracking usage).
         */
        private Integer tokens;

        /**
         * AI model used (if assistant message).
         */
        private String model;
    }
}
