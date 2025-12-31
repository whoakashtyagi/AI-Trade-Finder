package com.trade.app.controller;

import com.trade.app.openai.service.AIConversationService;
import com.trade.app.persistence.mongo.document.AIConversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for AI Conversation management.
 * 
 * This controller provides endpoints for managing multi-turn conversations
 * with OpenAI's Response API using conversation state and response IDs.
 * 
 * @author AI Trade Finder Team
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Slf4j
public class AIConversationController {
    
    private final AIConversationService conversationService;
    
    /**
     * Creates a new conversation session.
     * 
     * POST /api/v1/conversations
     * 
     * Example request body:
     * {
     *   "conversationType": "TRADE_ANALYSIS",
     *   "symbol": "NQ",
     *   "userId": "user123",
     *   "expiryHours": 24
     * }
     * 
     * @param request The conversation creation request
     * @return The created conversation
     */
    @PostMapping
    public ResponseEntity<AIConversation> createConversation(
            @RequestBody CreateConversationRequest request) {
        
        log.info("Creating new conversation: type={}, symbol={}", 
                request.getConversationType(), request.getSymbol());
        
        AIConversation conversation = conversationService.createConversation(
                request.getConversationType(),
                request.getSymbol(),
                request.getUserId(),
                request.getExpiryHours()
        );
        
        return ResponseEntity.ok(conversation);
    }
    
    /**
     * Gets a conversation by ID.
     * 
     * GET /api/v1/conversations/{conversationId}
     * 
     * @param conversationId The conversation ID
     * @return The conversation
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<AIConversation> getConversation(
            @PathVariable String conversationId) {
        
        return conversationService.getConversation(conversationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Gets conversation statistics.
     * 
     * GET /api/v1/conversations/{conversationId}/stats
     * 
     * @param conversationId The conversation ID
     * @return Conversation statistics
     */
    @GetMapping("/{conversationId}/stats")
    public ResponseEntity<Map<String, Object>> getConversationStats(
            @PathVariable String conversationId) {
        
        Map<String, Object> stats = conversationService.getConversationStats(conversationId);
        
        if (stats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Gets active conversations for a symbol.
     * 
     * GET /api/v1/conversations/symbol/{symbol}
     * 
     * @param symbol The trading symbol
     * @return List of active conversations
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<AIConversation>> getConversationsForSymbol(
            @PathVariable String symbol) {
        
        List<AIConversation> conversations = 
                conversationService.findActiveConversationsForSymbol(symbol);
        
        return ResponseEntity.ok(conversations);
    }
    
    /**
     * Marks a conversation as completed.
     * 
     * POST /api/v1/conversations/{conversationId}/complete
     * 
     * @param conversationId The conversation ID
     * @return Success response
     */
    @PostMapping("/{conversationId}/complete")
    public ResponseEntity<Map<String, String>> completeConversation(
            @PathVariable String conversationId) {
        
        conversationService.completeConversation(conversationId);
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Conversation marked as completed"
        ));
    }
    
    /**
     * Cleans up expired conversations.
     * 
     * POST /api/v1/conversations/cleanup
     * 
     * @return Number of conversations cleaned up
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpired() {
        
        int count = conversationService.cleanupExpiredConversations();
        
        return ResponseEntity.ok(Map.of(
                "cleanedUp", count,
                "message", count + " expired conversations cleaned up"
        ));
    }
    
    /**
     * Request DTO for creating conversations.
     */
    @lombok.Data
    public static class CreateConversationRequest {
        private String conversationType;
        private String symbol;
        private String userId;
        private Integer expiryHours;
    }
}
