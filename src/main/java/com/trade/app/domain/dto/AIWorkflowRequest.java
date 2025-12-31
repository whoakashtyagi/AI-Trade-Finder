package com.trade.app.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for AI workflow execution.
 * 
 * This request allows users to select predefined prompts or provide custom ones,
 * along with market data configuration for multi-timeframe analysis.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIWorkflowRequest {
    
    /**
     * Type of prompt to use: "PREDEFINED" or "CUSTOM".
     */
    private String promptType;
    
    /**
     * Key of the selected predefined prompt (if promptType is "PREDEFINED").
     * Examples: "day_analysis", "swing_trade", "risk_assessment"
     */
    private String selectedPredefinedPrompt;
    
    /**
     * Custom prompt text (if promptType is "CUSTOM").
     */
    private String customPromptText;
    
    /**
     * Additional user-provided context to append to the prompt.
     * This can include specific questions, constraints, or preferences.
     */
    private String additionalContext;
    
    /**
     * Trading symbol to analyze (e.g., "AAPL", "TSLA").
     */
    private String symbol;
    
    /**
     * Map of timeframe (e.g., "5m", "1h", "1d") to its specific configuration.
     * Each timeframe can have different time ranges and data inclusion settings.
     */
    private Map<String, TimeFrameConfig> timeframeSettings;
    
    /**
     * If true, returns the generated prompt without calling the AI service.
     * Useful for debugging and prompt engineering.
     */
    @JsonAlias({"dryRun", "isDryRun", "DryRun"})
    private boolean dryRun;
    
    /**
     * Optional manual dataset to include in the analysis.
     * Can be used to provide additional context or data points.
     */
    private List<Map<String, Object>> manualDataset;
    
    /**
     * Optional conversation ID for multi-turn conversations.
     * If provided, the service will use the previous response ID
     * from this conversation to maintain context across requests.
     */
    private String conversationId;
}
