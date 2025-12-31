package com.trade.app.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.app.datasource.DataSource;
import com.trade.app.datasource.factory.DataSourceFactory;
import com.trade.app.datasource.model.DataSourceConfig;
import com.trade.app.datasource.model.DataSourceResult;
import com.trade.app.domain.dto.AIWorkflowRequest;
import com.trade.app.domain.dto.TimeFrameConfig;
import com.trade.app.openai.client.AIClientService;
import com.trade.app.openai.dto.AIRequestDTO;
import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.openai.exception.AIClientException;
import com.trade.app.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Service for executing AI workflow requests with market data.
 * 
 * This service builds comprehensive prompts by combining:
 * - Predefined or custom prompt templates
 * - User-provided context
 * - Multi-timeframe market data from various data sources
 * - Optional manual datasets
 * 
 * Architecture:
 * - Uses Strategy Pattern via DataSource abstraction
 * - Data sources are injected via DataSourceFactory
 * - Easily extensible to new data source types
 * 
 * @author AI Trade Finder Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIWorkflowService {
    
    private final AIClientService aiClientService;
    private final DataSourceFactory dataSourceFactory;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final AIConversationService conversationService;
    
    // Mapping: Prompt key -> Resource file path
    private static final Map<String, String> PROMPT_FILES = new LinkedHashMap<>();
    
    static {
        PROMPT_FILES.put("day_analysis", "classpath:prompts/day_analysis.txt");
        PROMPT_FILES.put("swing_trade", "classpath:prompts/swing_trade.txt");
        PROMPT_FILES.put("risk_assessment", "classpath:prompts/risk_assessment.txt");
        PROMPT_FILES.put("market_structure", "classpath:prompts/market_structure.txt");
        PROMPT_FILES.put("entry_exit", "classpath:prompts/entry_exit.txt");
        PROMPT_FILES.put("general_analyzer", "classpath:prompts/general_analyzer.txt");
    }
    
    /**
     * Returns available predefined prompts with their display names.
     * 
     * @return Map of prompt keys to display names
     */
    public Map<String, String> getAvailablePrompts() {
        Map<String, String> options = new LinkedHashMap<>();
        options.put("day_analysis", "Day Trading Analysis");
        options.put("swing_trade", "Swing Trading Strategy");
        options.put("risk_assessment", "Risk Assessment & Position Sizing");
        options.put("market_structure", "Market Structure Analysis");
        options.put("entry_exit", "Entry & Exit Planning");
        options.put("general_analyzer", "General Market Analyzer");
        return options;
    }
    
    /**
     * Executes an AI workflow request and returns the analysis or generated prompt.
     * 
     * @param request The workflow request configuration
     * @return AI analysis response or generated prompt (if dry run)
     * @throws AIClientException if the AI service call fails
     */
    public AIResponseDTO executeWorkflow(AIWorkflowRequest request) throws AIClientException {
        StringBuilder finalPrompt = new StringBuilder();
        
        // Build System Instructions Section
        finalPrompt.append("### SYSTEM INSTRUCTIONS ###\n");
        if (Constants.PromptType.PREDEFINED.equalsIgnoreCase(request.getPromptType())) {
            String promptContent = loadPromptContent(request.getSelectedPredefinedPrompt());
            finalPrompt.append(promptContent).append("\n");
        } else {
            finalPrompt.append(request.getCustomPromptText()).append("\n");
        }
        
        // Append User Context
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().trim().isEmpty()) {
            finalPrompt.append("\n### USER PROVIDED CONTEXT ###\n");
            finalPrompt.append(request.getAdditionalContext()).append("\n");
        }
        
        // Append Manual Dataset
        if (request.getManualDataset() != null && !request.getManualDataset().isEmpty()) {
            finalPrompt.append("\n### MANUAL INPUT DATA ###\n");
            try {
                finalPrompt.append(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(request.getManualDataset()));
                finalPrompt.append("\n");
            } catch (JsonProcessingException e) {
                log.error("Error serializing manual dataset", e);
                finalPrompt.append("[]\n");
            }
        }
        
        // Append Market Data from all configured data sources
        finalPrompt.append("\n### MARKET DATA FOR ANALYSIS (").append(request.getSymbol()).append(") ###\n");
        
        if (request.getTimeframeSettings() != null && !request.getTimeframeSettings().isEmpty()) {
            for (Map.Entry<String, TimeFrameConfig> entry : request.getTimeframeSettings().entrySet()) {
                String timeframe = entry.getKey();
                TimeFrameConfig config = entry.getValue();
                
                if (!config.isEnabled()) {
                    log.debug("Timeframe {} is disabled, skipping", timeframe);
                    continue;
                }
                
                finalPrompt.append("\n--- TIMEFRAME: ").append(timeframe).append(" ---\n");
                
                // Process all data sources for this timeframe
                if (config.getDataSources() != null && !config.getDataSources().isEmpty()) {
                    for (DataSourceConfig dsConfig : config.getDataSources()) {
                        if (!dsConfig.isEnabled()) {
                            continue;
                        }
                        
                        try {
                            DataSource dataSource = dataSourceFactory.getDataSource(dsConfig.getDataSourceType());
                            DataSourceResult result = dataSource.fetchData(request.getSymbol(), timeframe, dsConfig);
                            
                            appendDataSourceResult(finalPrompt, result);
                            
                        } catch (Exception e) {
                            log.error("Error fetching data from source: {}", dsConfig.getDataSourceType(), e);
                            finalPrompt.append("\nError fetching ")
                                    .append(dsConfig.getDataSourceType().getDisplayName())
                                    .append(": ").append(e.getMessage()).append("\n");
                        }
                    }
                }
                
                finalPrompt.append("\n");
            }
        }
        
        // Handle Dry Run
        if (request.isDryRun()) {
            log.info("Dry run enabled. Returning generated prompt for symbol: {}", request.getSymbol());
            
            return AIResponseDTO.builder()
                    .requestId(UUID.randomUUID().toString())
                    .status("dry_run")
                    .output(finalPrompt.toString())
                    .model("N/A")
                    .build();
        }
        
        // Execute AI Request
        try {
            // Build AI request with conversation support
            AIRequestDTO.AIRequestDTOBuilder aiRequestBuilder = AIRequestDTO.builder()
                    .input(finalPrompt.toString())
                    .systemInstructions("You are an expert trading analyst. Analyze the provided market data and provide actionable insights.")
                    .maxTokens(2000)
                    .temperature(0.7)
                    .store(true); // Enable response storage for future retrieval
            
            // Add metadata for tracking
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("symbol", request.getSymbol());
            metadata.put("workflowType", "market_analysis");
            metadata.put("timestamp", Instant.now().toString());
            if (request.getSelectedPredefinedPrompt() != null) {
                metadata.put("promptType", request.getSelectedPredefinedPrompt());
            }
            aiRequestBuilder.metadata(metadata);
            
            // Check if this is a continuation of an existing conversation
            if (request.getConversationId() != null) {
                String previousResponseId = conversationService.getLatestResponseId(request.getConversationId());
                if (previousResponseId != null) {
                    aiRequestBuilder.previousResponseId(previousResponseId);
                    log.info("Continuing conversation {} with previous response {}", 
                            request.getConversationId(), previousResponseId);
                }
            }
            
            AIRequestDTO aiRequest = aiRequestBuilder.build();
            AIResponseDTO response = aiClientService.sendReasoningRequest(aiRequest);
            
            // If conversation tracking is enabled, save the turn
            if (request.getConversationId() != null && response.getId() != null) {
                conversationService.addTurn(
                        request.getConversationId(),
                        response,
                        response.getRequestId(),
                        "Market analysis for " + request.getSymbol()
                );
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("AI Service call failed for symbol: {}", request.getSymbol(), e);
            throw new AIClientException("AI workflow execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loads prompt content from resource files.
     * 
     * @param promptKey The key identifying the prompt file
     * @return The prompt content as string
     */
    private String loadPromptContent(String promptKey) {
        String resourcePath = PROMPT_FILES.get(promptKey);
        if (resourcePath == null) {
            log.warn("No prompt file configured for key: {}", promptKey);
            return "Analyze the market data for " + promptKey + " strategy.";
        }
        
        try {
            Resource resource = resourceLoader.getResource(resourcePath);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt file: {}", resourcePath, e);
            return "Error: Could not load strategy instructions for " + promptKey;
        }
    }
    
    /**
     * Appends data source result to the prompt in a formatted way.
     * 
     * @param sb The StringBuilder to append to
     * @param result The data source result
     */
    private void appendDataSourceResult(StringBuilder sb, DataSourceResult result) {
        sb.append("\n").append(result.getDataSourceType().getDisplayName()).append(":\n");
        
        if (!result.isSuccess()) {
            sb.append("Error: ").append(result.getErrorMessage()).append("\n");
            return;
        }
        
        if (result.getData() == null || result.getData().isEmpty()) {
            sb.append("No data available for this timeframe.\n");
            return;
        }
        
        try {
            // Add metadata first
            if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
                sb.append("Metadata:\n");
                sb.append(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(result.getMetadata()));
                sb.append("\n\n");
            }
            
            // Add data records
            sb.append("Data (").append(result.getRecordCount()).append(" records):\n");
            sb.append(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result.getData()));
            sb.append("\n");
            
        } catch (JsonProcessingException e) {
            log.error("Error serializing data source result", e);
            sb.append("[Error formatting data]\n");
        }
    }
}
