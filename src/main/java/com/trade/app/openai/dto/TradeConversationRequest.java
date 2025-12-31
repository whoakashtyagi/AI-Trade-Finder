package com.trade.app.openai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for creating trade-focused conversations.
 * 
 * This simplifies the creation of conversations about specific trades,
 * allowing traders to easily start asking follow-up questions.
 * 
 * Example usage:
 * <pre>
 * TradeConversationRequest request = TradeConversationRequest.builder()
 *     .tradeId("673abc123...")
 *     .userId("trader-001")
 *     .initialQuestion("Why is this a good entry point?")
 *     .includeMarketContext(true)
 *     .build();
 * </pre>
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeConversationRequest {
    
    /**
     * The identified trade ID to discuss.
     * Required.
     */
    private String tradeId;
    
    /**
     * User identifier.
     */
    private String userId;
    
    /**
     * Optional initial question to start the conversation.
     * If not provided, a default greeting with trade summary will be generated.
     */
    private String initialQuestion;
    
    /**
     * Whether to include current market context in the conversation.
     * If true, will fetch latest market data for the symbol.
     */
    @Builder.Default
    private Boolean includeMarketContext = false;
    
    /**
     * User's risk tolerance (e.g., "conservative", "moderate", "aggressive").
     * Used to tailor AI responses.
     */
    private String riskTolerance;
    
    /**
     * User's preferred trading style (e.g., "scalp", "day", "swing").
     */
    private String tradingStyle;
    
    /**
     * Additional context or preferences to include in the conversation.
     */
    private Map<String, Object> additionalContext;
    
    /**
     * Hours until conversation expires.
     * Defaults to 24 hours if not specified.
     */
    private Integer expiryHours;
}
