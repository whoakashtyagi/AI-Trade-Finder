package com.trade.app.openai.service;

import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.persistence.mongo.document.AIConversation;
import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import com.trade.app.persistence.mongo.repository.AIConversationRepository;
import com.trade.app.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Generic service for managing AI conversation state and multi-turn interactions.
 * 
 * This service provides conversation management for traders to have
 * follow-up discussions about identified trades, market analysis, and
 * trading strategies using OpenAI's Response API with conversation continuity.
 * 
 * Key Features:
 * - Create conversations linked to specific trades or entities
 * - Multi-turn dialogue with context preservation
 * - Trade-specific conversation templates and contexts
 * - Flexible entity reference system for extensibility
 * - Conversation lifecycle management
 * 
 * Trader Use Cases:
 * - "Why is this a good entry point?"
 * - "What if price breaks below the stop?"
 * - "How does this compare to yesterday's setup?"
 * - "What's the risk-reward for partial exits?"
 * 
 * @author AI Trade Finder Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIConversationService {
    
    private final AIConversationRepository conversationRepository;
    
    // Default conversation expiry: 24 hours
    private static final int DEFAULT_EXPIRY_HOURS = 24;
    
    // ========== Trade-Focused Conversation Creation ==========
    
    /**
     * Creates a conversation about a specific identified trade.
     * This is the primary method for traders to start asking questions about a trade opportunity.
     * 
     * @param trade The identified trade to discuss
     * @param userId User identifier
     * @param expiryHours Hours until conversation expires
     * @return The created conversation with trade context
     */
    public AIConversation createTradeConversation(
            IdentifiedTrade trade,
            String userId,
            Integer expiryHours) {
        
        String conversationId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        int expiry = expiryHours != null ? expiryHours : DEFAULT_EXPIRY_HOURS;
        
        // Build entity snapshot from trade data
        Map<String, Object> entitySnapshot = buildTradeSnapshot(trade);
        
        // Build conversation context with technical levels and market structure
        Map<String, Object> contextData = buildTradeContext(trade);
        
        // Generate relevant tags
        List<String> tags = generateTradeTags(trade);
        
        AIConversation conversation = AIConversation.builder()
                .conversationId(conversationId)
                .conversationType("TRADE_FOLLOWUP")
                .symbol(trade.getSymbol())
                .userId(userId)
                .tradeId(trade.getId())
                .entityType("TRADE")
                .entityId(trade.getId())
                .entitySnapshot(entitySnapshot)
                .contextData(contextData)
                .tags(tags)
                .status("ACTIVE")
                .createdAt(now)
                .lastActivityAt(now)
                .expiresAt(now.plus(expiry, ChronoUnit.HOURS))
                .turns(new ArrayList<>())
                .metadata(new HashMap<>())
                .build();
        
        AIConversation saved = conversationRepository.save(conversation);
        log.info("Created trade conversation: {} for trade: {} (symbol: {}, direction: {}, confidence: {})",
                conversationId, trade.getId(), trade.getSymbol(), trade.getDirection(), trade.getConfidence());
        
        return saved;
    }
    
    /**
     * Finds or creates an active conversation for a specific trade.
     * Prevents duplicate conversations for the same trade.
     * 
     * @param trade The identified trade
     * @param userId User identifier
     * @return Existing or new conversation
     */
    public AIConversation getOrCreateTradeConversation(IdentifiedTrade trade, String userId) {
        Optional<AIConversation> existing = conversationRepository.findByTradeIdAndStatus(
                trade.getId(), "ACTIVE");

        if (existing.isPresent()) {
            log.debug("Found existing conversation for trade: {}", trade.getId());
            return existing.get();
        }
        
        return createTradeConversation(trade, userId, null);
    }
    
    /**
     * Creates a new generic conversation session.
     * For trade-specific conversations, prefer using createTradeConversation().
     * 
     * @param conversationType Type of conversation
     * @param symbol Optional symbol this conversation is about
     * @param userId Optional user identifier
     * @param expiryHours Hours until conversation expires
     * @return The created conversation
     */
    public AIConversation createConversation(
            String conversationType,
            String symbol,
            String userId,
            Integer expiryHours) {
        
        String conversationId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        int expiry = expiryHours != null ? expiryHours : DEFAULT_EXPIRY_HOURS;
        
        AIConversation conversation = AIConversation.builder()
                .conversationId(conversationId)
                .conversationType(conversationType)
                .symbol(symbol)
                .userId(userId)
                .status("ACTIVE")
                .createdAt(now)
                .lastActivityAt(now)
                .expiresAt(now.plus(expiry, ChronoUnit.HOURS))
                .turns(new ArrayList<>())
                .metadata(new HashMap<>())
                .contextData(new HashMap<>())
                .build();
        
        AIConversation saved = conversationRepository.save(conversation);
        log.info("Created AI conversation: {} (type: {}, symbol: {})", 
                conversationId, conversationType, symbol);
        
        return saved;
    }
    
    /**
     * Adds a turn to an existing conversation.
     * 
     * @param conversationId The conversation ID
     * @param aiResponse The AI response from this turn
     * @param requestId The request ID that generated this response
     * @param userInputSummary Brief summary of user input
     * @return Updated conversation
     */
    public AIConversation addTurn(
            String conversationId,
            AIResponseDTO aiResponse,
            String requestId,
            String userInputSummary) {
        
        Optional<AIConversation> optConv = conversationRepository.findByConversationId(conversationId);
        if (optConv.isEmpty()) {
            log.warn("Conversation not found: {}", conversationId);
            return null;
        }
        
        AIConversation conversation = optConv.get();
        
        // Create turn
        AIConversation.ConversationTurn turn = AIConversation.ConversationTurn.builder()
                .responseId(aiResponse.getId())
                .requestId(requestId)
                .userInputSummary(summarize(userInputSummary, 200))
                .aiOutputSummary(summarize(aiResponse.getOutput(), 200))
                .model(aiResponse.getModel())
                .timestamp(Instant.now())
                .tokensUsed(aiResponse.getUsage() != null ? aiResponse.getUsage().getTotalTokens() : null)
                .metadata(new HashMap<>())
                .build();
        
        conversation.addTurn(turn);
        
        AIConversation saved = conversationRepository.save(conversation);
        log.debug("Added turn to conversation {}. Response ID: {}", conversationId, aiResponse.getId());
        
        return saved;
    }
    
    /**
     * Gets the latest response ID from a conversation for use as previousResponseId.
     * 
     * @param conversationId The conversation ID
     * @return The latest response ID, or null if not found
     */
    public String getLatestResponseId(String conversationId) {
        Optional<AIConversation> optConv = conversationRepository.findByConversationId(conversationId);
        if (optConv.isEmpty()) {
            log.warn("Conversation not found: {}", conversationId);
            return null;
        }
        
        AIConversation conversation = optConv.get();
        String responseId = conversation.getLatestResponseId();
        
        log.debug("Latest response ID for conversation {}: {}", conversationId, responseId);
        return responseId;
    }
    
    /**
     * Gets a conversation by ID.
     * 
     * @param conversationId The conversation ID
     * @return Optional containing the conversation
     */
    public Optional<AIConversation> getConversation(String conversationId) {
        return conversationRepository.findByConversationId(conversationId);
    }
    
    /**
     * Marks a conversation as completed.
     * 
     * @param conversationId The conversation ID
     */
    public void completeConversation(String conversationId) {
        Optional<AIConversation> optConv = conversationRepository.findByConversationId(conversationId);
        if (optConv.isEmpty()) {
            log.warn("Conversation not found: {}", conversationId);
            return;
        }
        
        AIConversation conversation = optConv.get();
        conversation.setStatus("COMPLETED");
        conversation.setLastActivityAt(Instant.now());
        
        conversationRepository.save(conversation);
        log.info("Marked conversation as completed: {}", conversationId);
    }
    
    /**
     * Finds active conversations for a symbol.
     * 
     * @param symbol The trading symbol
     * @return List of active conversations
     */
    public List<AIConversation> findActiveConversationsForSymbol(String symbol) {
        return conversationRepository.findBySymbolAndStatus(symbol, Constants.ConversationStatus.ACTIVE);
    }
    
    /**
     * Cleans up expired conversations.
     * 
     * @return Number of conversations cleaned up
     */
    public int cleanupExpiredConversations() {
        Instant now = Instant.now();
        List<AIConversation> expired = conversationRepository.findByExpiresAtBefore(now);
        
        if (!expired.isEmpty()) {
            for (AIConversation conv : expired) {
                conv.setStatus(Constants.ConversationStatus.EXPIRED);
            }
            conversationRepository.saveAll(expired);
            log.info("Cleaned up {} expired conversations", expired.size());
        }
        
        return expired.size();
    }
    
    /**
     * Gets conversation statistics.
     * 
     * @param conversationId The conversation ID
     * @return Map of statistics
     */
    public Map<String, Object> getConversationStats(String conversationId) {
        Optional<AIConversation> optConv = conversationRepository.findByConversationId(conversationId);
        if (optConv.isEmpty()) {
            return Collections.emptyMap();
        }
        
        AIConversation conversation = optConv.get();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("conversationId", conversation.getConversationId());
        stats.put("turnCount", conversation.getTurns().size());
        stats.put("createdAt", conversation.getCreatedAt());
        stats.put("lastActivity", conversation.getLastActivityAt());
        stats.put("status", conversation.getStatus());
        
        // Calculate total tokens
        int totalTokens = conversation.getTurns().stream()
                .mapToInt(turn -> turn.getTokensUsed() != null ? turn.getTokensUsed() : 0)
                .sum();
        stats.put("totalTokens", totalTokens);
        
        return stats;
    }
    
    /**
     * Summarizes text to a maximum length.
     */
    private String summarize(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
    
    // ========== Trade-Specific Query Methods ==========
    
    /**
     * Finds all conversations about a specific trade.
     * 
     * @param tradeId The identified trade ID
     * @return List of conversations about this trade
     */
    public List<AIConversation> findConversationsForTrade(String tradeId) {
        return conversationRepository.findByTradeId(tradeId);
    }
    
    /**
     * Finds the active conversation for a trade (if any).
     * 
     * @param tradeId The identified trade ID
     * @return Optional containing the active conversation
     */
    public Optional<AIConversation> findActiveConversationForTrade(String tradeId) {
        return conversationRepository.findByTradeIdAndStatus(tradeId, Constants.ConversationStatus.ACTIVE);
    }
    
    /**
     * Finds conversations by entity type.
     * 
     * @param entityType The entity type (\"TRADE\", \"SYMBOL\", \"PATTERN\", etc.)
     * @param status Optional status filter
     * @return List of conversations
     */
    public List<AIConversation> findConversationsByEntityType(String entityType, String status) {
        if (status != null) {
            return conversationRepository.findByEntityTypeAndStatus(entityType, status);
        }
        return conversationRepository.findByEntityTypeAndEntityId(entityType, null);
    }
    
    /**
     * Finds conversations by tag (e.g., \"high-confidence\", \"scalp-setup\").
     * 
     * @param tag The tag to search for
     * @return List of conversations with this tag
     */
    public List<AIConversation> findConversationsByTag(String tag) {
        return conversationRepository.findByTagsContaining(tag);
    }
    
    /**
     * Updates conversation context data dynamically.
     * Useful for adding real-time market data or updated levels during conversation.
     * 
     * @param conversationId The conversation ID
     * @param key Context data key
     * @param value Context data value
     */
    public void updateContextData(String conversationId, String key, Object value) {
        Optional<AIConversation> optConv = conversationRepository.findByConversationId(conversationId);
        if (optConv.isEmpty()) {
            log.warn("Conversation not found: {}", conversationId);
            return;
        }
        
        AIConversation conversation = optConv.get();
        if (conversation.getContextData() == null) {
            conversation.setContextData(new HashMap<>());
        }
        conversation.getContextData().put(key, value);
        conversation.setLastActivityAt(Instant.now());
        
        conversationRepository.save(conversation);
        log.debug("Updated context data for conversation {}: {} = {}", conversationId, key, value);
    }
    
    /**
     * Gets the conversation context summary for LLM prompts.
     * Returns a formatted string with trade details and context.
     * 
     * @param conversationId The conversation ID
     * @return Formatted context string
     */
    public String getConversationContextSummary(String conversationId) {
        Optional<AIConversation> optConv = conversationRepository.findByConversationId(conversationId);
        if (optConv.isEmpty()) {
            return null;
        }
        
        AIConversation conv = optConv.get();
        StringBuilder context = new StringBuilder();
        
        context.append("=== Trade Context ===\\n");
        
        if (conv.getEntitySnapshot() != null) {
            conv.getEntitySnapshot().forEach((key, value) -> 
                context.append(key).append(": ").append(value).append("\\n")
            );
        }
        
        if (conv.getContextData() != null && !conv.getContextData().isEmpty()) {
            context.append("\\n=== Additional Context ===\\n");
            conv.getContextData().forEach((key, value) -> 
                context.append(key).append(": ").append(value).append("\\n")
            );
        }
        
        return context.toString();
    }
    
    // ========== Helper Methods for Building Trade Context ==========
    
    /**
     * Builds an entity snapshot from an identified trade.
     * Captures the complete trade setup at conversation start.
     */
    private Map<String, Object> buildTradeSnapshot(IdentifiedTrade trade) {
        Map<String, Object> snapshot = new HashMap<>();
        
        snapshot.put("tradeId", trade.getId());
        snapshot.put("symbol", trade.getSymbol());
        snapshot.put("direction", trade.getDirection());
        snapshot.put("confidence", trade.getConfidence());
        snapshot.put("entryZone", trade.getEntryZone());
        snapshot.put("entryPrice", trade.getEntryPrice());
        snapshot.put("entryZoneType", trade.getEntryZoneType());
        snapshot.put("stopPlacement", trade.getStopPlacement());
        snapshot.put("targets", trade.getTargets());
        snapshot.put("rrHint", trade.getRrHint());
        snapshot.put("timeframe", trade.getTimeframe());
        snapshot.put("sessionLabel", trade.getSessionLabel());
        snapshot.put("narrative", trade.getNarrative());
        snapshot.put("triggerConditions", trade.getTriggerConditions());
        snapshot.put("invalidations", trade.getInvalidations());
        snapshot.put("identifiedAt", trade.getIdentifiedAt());
        
        return snapshot;
    }
    
    /**
     * Builds initial conversation context for a trade.
     * Adds market structure, key levels, and technical context.
     */
    private Map<String, Object> buildTradeContext(IdentifiedTrade trade) {
        Map<String, Object> context = new HashMap<>();
        
        // Add current market structure hints
        context.put("marketDirection", trade.getDirection());
        context.put("activeSession", trade.getSessionLabel());
        context.put("primaryTimeframe", trade.getTimeframe());
        
        // Add price context
        if (trade.getEntryPrice() != null) {
            context.put("entryLevel", trade.getEntryPrice());
        }
        
        // Add setup type context
        if (trade.getEntryZoneType() != null) {
            context.put("setupType", trade.getEntryZoneType());
            context.put("setupDescription", describeSetupType(trade.getEntryZoneType()));
        }
        
        // Mark conversation as ready for questions
        context.put("readyForQuestions", true);
        
        return context;
    }
    
    /**
     * Generates relevant tags for a trade conversation.
     */
    private List<String> generateTradeTags(IdentifiedTrade trade) {
        List<String> tags = new ArrayList<>();
        
        tags.add(trade.getSymbol().toLowerCase());
        tags.add(trade.getDirection().toLowerCase());
        
        if (trade.getConfidence() != null) {
            if (trade.getConfidence() >= 80) {
                tags.add("high-confidence");
            } else if (trade.getConfidence() >= 60) {
                tags.add("medium-confidence");
            }
        }
        
        if (trade.getEntryZoneType() != null) {
            tags.add(trade.getEntryZoneType().toLowerCase().replace("_", "-"));
        }
        
        if (trade.getSessionLabel() != null) {
            tags.add(trade.getSessionLabel().toLowerCase().replace("_", "-"));
        }
        
        if (trade.getTimeframe() != null) {
            tags.add("tf-" + trade.getTimeframe().toLowerCase());
        }
        
        return tags;
    }
    
    /**
     * Provides a human-readable description of a setup type.
     */
    private String describeSetupType(String setupType) {
        if (Constants.EntryZoneType.FVG_CE.equals(setupType)) {
            return "Fair Value Gap at Consequent Encroachment (50% level)";
        } else if (Constants.EntryZoneType.IFVG.equals(setupType)) {
            return "Inverted Fair Value Gap";
        } else if (Constants.EntryZoneType.OB.equals(setupType)) {
            return "Order Block (institutional demand/supply zone)";
        } else if (Constants.EntryZoneType.BREAKER.equals(setupType)) {
            return "Breaker Block (failed order block turned trap)";
        } else if (Constants.EntryZoneType.MITIGATION.equals(setupType)) {
            return "Mitigation Block (unfilled inefficiency)";
        } else {
            return setupType;
        }
    }
}
