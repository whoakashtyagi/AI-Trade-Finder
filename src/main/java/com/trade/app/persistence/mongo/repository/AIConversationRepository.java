package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.AIConversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AI Conversation documents.
 * 
 * Manages persistence of conversation state including response IDs
 * for multi-turn conversations with OpenAI's Response API.
 * 
 * @author AI Trade Finder Team
 */
@Repository
public interface AIConversationRepository extends MongoRepository<AIConversation, String> {
    
    /**
     * Finds a conversation by its conversation ID.
     * 
     * @param conversationId The conversation ID
     * @return Optional containing the conversation if found
     */
    Optional<AIConversation> findByConversationId(String conversationId);
    
    /**
     * Finds active conversations for a specific symbol.
     * 
     * @param symbol The trading symbol
     * @param status The conversation status
     * @return List of active conversations
     */
    List<AIConversation> findBySymbolAndStatus(String symbol, String status);
    
    /**
     * Finds conversations by type and status.
     * 
     * @param conversationType The type of conversation
     * @param status The conversation status
     * @return List of matching conversations
     */
    List<AIConversation> findByConversationTypeAndStatus(String conversationType, String status);
    
    /**
     * Finds expired conversations for cleanup.
     * 
     * @param now The current timestamp
     * @return List of expired conversations
     */
    List<AIConversation> findByExpiresAtBefore(Instant now);
    
    /**
     * Finds conversations for a specific user.
     * 
     * @param userId The user identifier
     * @param status Optional status filter
     * @return List of user's conversations
     */
    List<AIConversation> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Finds recent conversations (for active session management).
     * 
     * @param cutoffTime Conversations active after this time
     * @return List of recent conversations
     */
    List<AIConversation> findByLastActivityAtAfter(Instant cutoffTime);
    
    /**
     * Finds conversations by trade ID.
     * 
     * @param tradeId The identified trade document ID
     * @return List of conversations about this trade
     */
    List<AIConversation> findByTradeId(String tradeId);
    
    /**
     * Finds active conversation for a specific trade.
     * 
     * @param tradeId The identified trade document ID
     * @param status The conversation status
     * @return Optional containing the active conversation
     */
    Optional<AIConversation> findByTradeIdAndStatus(String tradeId, String status);
    
    /**
     * Finds conversations by entity type and ID.
     * 
     * @param entityType The type of entity
     * @param entityId The entity ID
     * @return List of conversations about this entity
     */
    List<AIConversation> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    /**
     * Finds active conversations by entity type.
     * 
     * @param entityType The type of entity
     * @param status The conversation status
     * @return List of active conversations
     */
    List<AIConversation> findByEntityTypeAndStatus(String entityType, String status);
    
    /**
     * Finds conversations by tags.
     * 
     * @param tag The tag to search for
     * @return List of conversations with this tag
     */
    List<AIConversation> findByTagsContaining(String tag);
}
