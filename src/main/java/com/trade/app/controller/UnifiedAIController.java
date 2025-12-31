package com.trade.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.app.domain.dto.TradeSignalResponseDTO;
import com.trade.app.openai.client.AIClientService;
import com.trade.app.openai.dto.AIRequestDTO;
import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.persistence.mongo.document.ChatSession;
import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import com.trade.app.persistence.mongo.repository.ChatSessionRepository;
import com.trade.app.persistence.mongo.repository.IdentifiedTradeRepository;
import com.trade.app.service.OperationLogService;
import com.trade.app.util.OperationLogContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unified AI Controller with structured outputs and chat capabilities.
 * 
 * Features:
 * - OpenAI structured output support
 * - Multi-turn chat sessions
 * - Trade-specific discussions
 * - Market analysis
 * - Generic AI requests
 * 
 * @author AI Trade Finder Team
 */
@RestController
@RequestMapping("/api/v2/ai")
@RequiredArgsConstructor
@Slf4j
public class UnifiedAIController {

    private final AIClientService aiClientService;
    private final ChatSessionRepository chatSessionRepository;
    private final IdentifiedTradeRepository identifiedTradeRepository;
    private final ObjectMapper objectMapper;
    private final OperationLogService operationLogService;

    // ========== Structured Output Endpoints ==========

    /**
     * Request trade analysis with structured output.
     * 
     * POST /api/v2/ai/analyze/trade
     */
    @PostMapping("/analyze/trade")
    public ResponseEntity<TradeSignalResponseDTO> analyzeTradeStructured(
        @RequestBody AnalyzeTradeRequest request
    ) {
        String operationId = operationLogService.startOrReuse(
            "AI_ANALYZE_TRADE",
            "AI analyze trade: " + request.getSymbol(),
            "API",
            Map.of(
                "endpoint", "/api/v2/ai/analyze/trade",
                "symbol", request.getSymbol(),
                "timeframe", request.getTimeframe()
            )
        );
        try {
            log.info("Analyzing trade with structured output: {}", request.getSymbol());

            // Build AI request with structured output schema
            String systemPrompt = buildTradeAnalysisPrompt();
            String userInput = objectMapper.writeValueAsString(request);

            AIRequestDTO aiRequest = AIRequestDTO.builder()
                .input(userInput)
                .systemInstructions(systemPrompt)
                .maxTokens(4000)
                .temperature(0.7)
                .requestId("TRADE_ANALYSIS_" + System.currentTimeMillis())
                .build();

            AIResponseDTO response = aiClientService.sendReasoningRequest(aiRequest);

            // Parse structured output
            TradeSignalResponseDTO structuredOutput = parseStructuredOutput(
                response.getOutput(), TradeSignalResponseDTO.class
            );

            operationLogService.completeSuccess(operationId, "AI trade analysis completed", Map.of(
                "model", response.getModel(),
                "tokens", response.getUsage() != null ? response.getUsage().getTotalTokens() : null
            ));

            return ResponseEntity.ok(structuredOutput);

        } catch (Exception e) {
            log.error("Error in structured trade analysis", e);
            operationLogService.completeFailure(operationId, "AI trade analysis failed", e, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            OperationLogContext.clear();
        }
    }

    /**
     * Generic AI analysis with structured response format.
     * 
     * POST /api/v2/ai/analyze
     */
    @PostMapping("/analyze")
    public ResponseEntity<JsonNode> analyzeStructured(@RequestBody StructuredAnalysisRequest request) {
        String operationId = operationLogService.startOrReuse(
            "AI_ANALYZE",
            "AI structured analysis: " + request.getAnalysisType(),
            "API",
            Map.of(
                "endpoint", "/api/v2/ai/analyze",
                "analysisType", request.getAnalysisType()
            )
        );
        try {
            log.info("Processing structured analysis request: {}", request.getAnalysisType());

            AIRequestDTO aiRequest = AIRequestDTO.builder()
                .input(request.getInput())
                .systemInstructions(request.getSystemInstructions())
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 4000)
                .temperature(request.getTemperature() != null ? request.getTemperature() : 0.7)
                .build();

            AIResponseDTO response = aiClientService.sendReasoningRequest(aiRequest);

            // Parse as JSON
            JsonNode structuredOutput = objectMapper.readTree(response.getOutput());

            operationLogService.completeSuccess(operationId, "AI structured analysis completed", Map.of(
                "model", response.getModel(),
                "tokens", response.getUsage() != null ? response.getUsage().getTotalTokens() : null
            ));

            return ResponseEntity.ok(structuredOutput);

        } catch (Exception e) {
            log.error("Error in structured analysis", e);
            operationLogService.completeFailure(operationId, "AI structured analysis failed", e, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            OperationLogContext.clear();
        }
    }

    // ========== Chat Session Endpoints ==========

    /**
     * Create a new chat session.
     * 
     * POST /api/v2/ai/chat/sessions
     */
    @PostMapping("/chat/sessions")
    public ResponseEntity<ChatSession> createChatSession(@RequestBody CreateChatSessionRequest request) {
        try {
            log.info("Creating chat session: type={}, tradeId={}", request.getType(), request.getTradeId());

            // Build initial context
            String context = buildChatContext(request);

            ChatSession session = ChatSession.builder()
                .title(request.getTitle())
                .type(request.getType())
                .tradeId(request.getTradeId())
                .symbol(request.getSymbol())
                .context(context)
                .active(true)
                .messages(new ArrayList<>())
                .build();

            ChatSession saved = chatSessionRepository.save(session);

            log.info("Created chat session: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            log.error("Error creating chat session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send a message in a chat session.
     * 
     * POST /api/v2/ai/chat/sessions/{sessionId}/messages
     */
    @PostMapping("/chat/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageResponse> sendChatMessage(
        @PathVariable String sessionId,
        @RequestBody ChatMessageRequest request
    ) {
        try {
            log.info("Sending message to chat session: {}", sessionId);

            ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

            if (!session.isActive()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Add user message
            ChatSession.ChatMessage userMessage = ChatSession.ChatMessage.builder()
                .role("user")
                .content(request.getMessage())
                .timestamp(Instant.now())
                .build();
            
            session.getMessages().add(userMessage);

            // Build AI request with full conversation history
            AIRequestDTO aiRequest = buildChatAIRequest(session, request.getMessage());

            // Get AI response
            AIResponseDTO aiResponse = aiClientService.sendReasoningRequest(aiRequest);

            // Add assistant message
            ChatSession.ChatMessage assistantMessage = ChatSession.ChatMessage.builder()
                .role("assistant")
                .content(aiResponse.getOutput())
                .timestamp(Instant.now())
                .model(aiResponse.getModel())
                .tokens(aiResponse.getUsage() != null ? aiResponse.getUsage().getTotalTokens() : null)
                .build();

            session.getMessages().add(assistantMessage);
            session.setUpdatedAt(Instant.now());

            chatSessionRepository.save(session);

            ChatMessageResponse response = ChatMessageResponse.builder()
                .sessionId(sessionId)
                .message(assistantMessage.getContent())
                .timestamp(assistantMessage.getTimestamp())
                .model(assistantMessage.getModel())
                .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending chat message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get chat session by ID.
     * 
     * GET /api/v2/ai/chat/sessions/{sessionId}
     */
    @GetMapping("/chat/sessions/{sessionId}")
    public ResponseEntity<ChatSession> getChatSession(@PathVariable String sessionId) {
        return chatSessionRepository.findById(sessionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all chat sessions for a trade.
     * 
     * GET /api/v2/ai/chat/trades/{tradeId}/sessions
     */
    @GetMapping("/chat/trades/{tradeId}/sessions")
    public ResponseEntity<List<ChatSession>> getChatSessionsForTrade(@PathVariable String tradeId) {
        List<ChatSession> sessions = chatSessionRepository.findByTradeIdOrderByCreatedAtDesc(tradeId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Close a chat session.
     * 
     * POST /api/v2/ai/chat/sessions/{sessionId}/close
     */
    @PostMapping("/chat/sessions/{sessionId}/close")
    public ResponseEntity<Void> closeChatSession(@PathVariable String sessionId) {
        try {
            ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));

            session.setActive(false);
            session.setUpdatedAt(Instant.now());
            chatSessionRepository.save(session);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error closing chat session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active chat sessions.
     * 
     * GET /api/v2/ai/chat/sessions/active
     */
    @GetMapping("/chat/sessions/active")
    public ResponseEntity<List<ChatSession>> getActiveChatSessions() {
        List<ChatSession> sessions = chatSessionRepository.findByActiveOrderByUpdatedAtDesc(true);
        return ResponseEntity.ok(sessions);
    }

    // ========== Trade Discussion Helper ==========

    /**
     * Start a discussion about a specific identified trade.
     * 
     * POST /api/v2/ai/chat/discuss-trade
     */
    @PostMapping("/chat/discuss-trade")
    public ResponseEntity<ChatSession> discussTrade(@RequestBody DiscussTradeRequest request) {
        try {
            log.info("Starting discussion about trade: {}", request.getTradeId());

            // Get the trade
            IdentifiedTrade trade = identifiedTradeRepository.findById(request.getTradeId())
                .orElseThrow(() -> new RuntimeException("Trade not found"));

            // Build context with trade details
            String context = buildTradeDiscussionContext(trade);

            // Create chat session
            ChatSession session = ChatSession.builder()
                .title("Discussion: " + trade.getSymbol() + " " + trade.getDirection())
                .type("TRADE_DISCUSSION")
                .tradeId(request.getTradeId())
                .symbol(trade.getSymbol())
                .context(context)
                .active(true)
                .messages(new ArrayList<>())
                .build();

            ChatSession saved = chatSessionRepository.save(session);

            // If initial question provided, send it
            if (request.getInitialQuestion() != null && !request.getInitialQuestion().isBlank()) {
                ChatMessageRequest messageRequest = new ChatMessageRequest();
                messageRequest.setMessage(request.getInitialQuestion());
                sendChatMessage(saved.getId(), messageRequest);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            log.error("Error starting trade discussion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Helper Methods ==========

    private String buildTradeAnalysisPrompt() {
        return "You are an expert trading AI using Smart Money Concepts. " +
               "Analyze the provided market data and return a structured JSON response with trade signals. " +
               "Include: status, direction, confidence, entry zones, stops, targets, and narrative explanation.";
    }

    private String buildChatContext(CreateChatSessionRequest request) {
        if (request.getTradeId() != null) {
            IdentifiedTrade trade = identifiedTradeRepository.findById(request.getTradeId()).orElse(null);
            if (trade != null) {
                return buildTradeDiscussionContext(trade);
            }
        }
        return "General trading discussion session. User is interested in: " + request.getType();
    }

    private String buildTradeDiscussionContext(IdentifiedTrade trade) {
        return String.format(
            "Trade Setup Context:\n" +
            "Symbol: %s\n" +
            "Direction: %s\n" +
            "Confidence: %d%%\n" +
            "Entry Zone: %s\n" +
            "Stop: %s\n" +
            "Targets: %s\n" +
            "Narrative: %s\n" +
            "Session: %s\n" +
            "Timeframe: %s",
            trade.getSymbol(),
            trade.getDirection(),
            trade.getConfidence(),
            trade.getEntryZone(),
            trade.getStopPlacement(),
            trade.getTargets(),
            trade.getNarrative(),
            trade.getSessionLabel(),
            trade.getTimeframe()
        );
    }

    private AIRequestDTO buildChatAIRequest(ChatSession session, String newMessage) {
        // Build conversation history
        StringBuilder conversationHistory = new StringBuilder();
        conversationHistory.append("Session Context:\n").append(session.getContext()).append("\n\n");
        conversationHistory.append("Conversation History:\n");

        for (ChatSession.ChatMessage msg : session.getMessages()) {
            conversationHistory.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        return AIRequestDTO.builder()
            .input(conversationHistory.toString())
            .systemInstructions("You are an expert trading advisor helping analyze trades and market conditions. " +
                "Be concise, practical, and provide actionable insights based on Smart Money Concepts.")
            .maxTokens(2000)
            .temperature(0.7)
            .build();
    }

    private <T> T parseStructuredOutput(String output, Class<T> clazz) throws Exception {
        // Clean JSON if wrapped in markdown
        String cleanJson = output.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        return objectMapper.readValue(cleanJson.trim(), clazz);
    }

    // ========== Request/Response DTOs ==========

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyzeTradeRequest {
        private String symbol;
        private String timeframe;
        private Map<String, Object> marketData;
        private List<String> indicators;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredAnalysisRequest {
        private String analysisType;
        private String input;
        private String systemInstructions;
        private Integer maxTokens;
        private Double temperature;
        private String responseFormat; // "json_object" for structured output
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChatSessionRequest {
        private String title;
        private String type; // TRADE_DISCUSSION, MARKET_ANALYSIS, GENERAL
        private String tradeId;
        private String symbol;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageRequest {
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageResponse {
        private String sessionId;
        private String message;
        private Instant timestamp;
        private String model;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscussTradeRequest {
        private String tradeId;
        private String initialQuestion;
    }
}
