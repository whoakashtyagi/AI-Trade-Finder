package com.trade.app.openai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.app.datasource.factory.DataSourceFactory;
import com.trade.app.datasource.impl.CoreMarketEventDataSource;
import com.trade.app.datasource.model.DataSourceConfig;
import com.trade.app.datasource.model.DataSourceResult;
import com.trade.app.datasource.model.DataSourceType;
import com.trade.app.domain.dto.AIWorkflowRequest;
import com.trade.app.domain.dto.TimeFrameConfig;
import com.trade.app.openai.client.AIClientService;
import com.trade.app.openai.dto.AIResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AIWorkflowService.
 * 
 * @author AI Trade Finder Team
 */
@ExtendWith(MockitoExtension.class)
class AIWorkflowServiceTest {
    
    @Mock
    private AIClientService aiClientService;
    
    @Mock
    private DataSourceFactory dataSourceFactory;
    
    @Mock
    private CoreMarketEventDataSource coreMarketEventDataSource;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private ResourceLoader resourceLoader;
    
    @InjectMocks
    private AIWorkflowService aiWorkflowService;
    
    private AIWorkflowRequest testRequest;
    
    @BeforeEach
    void setUp() {
        // Setup test data
        Instant start = Instant.parse("2024-01-01T09:30:00Z");
        Instant end = Instant.parse("2024-01-01T16:00:00Z");
        
        DataSourceConfig dsConfig = DataSourceConfig.builder()
                .dataSourceType(DataSourceType.CORE_MARKET_EVENT)
                .enabled(true)
                .fromTime(start)
                .toTime(end)
                .maxRecords(10)
                .build();
        
        TimeFrameConfig config = TimeFrameConfig.builder()
                .enabled(true)
                .dataSources(Collections.singletonList(dsConfig))
                .build();
        
        Map<String, TimeFrameConfig> timeframeSettings = new HashMap<>();
        timeframeSettings.put("5m", config);
        
        testRequest = AIWorkflowRequest.builder()
                .promptType("PREDEFINED")
                .selectedPredefinedPrompt("day_analysis")
                .symbol("AAPL")
                .additionalContext("Looking for scalping opportunities")
                .timeframeSettings(timeframeSettings)
                .dryRun(false)
                .build();
    }
    
    @Test
    void testGetAvailablePrompts() {
        Map<String, String> prompts = aiWorkflowService.getAvailablePrompts();
        
        assertNotNull(prompts);
        assertFalse(prompts.isEmpty());
        assertTrue(prompts.containsKey("day_analysis"));
        assertTrue(prompts.containsKey("swing_trade"));
        assertTrue(prompts.containsKey("general_analyzer"));
    }
    
    @Test
    void testExecuteWorkflowDryRun() throws Exception {
        testRequest.setDryRun(true);
        
        // Mock data source result
        DataSourceResult mockResult = DataSourceResult.builder()
                .dataSourceType(DataSourceType.CORE_MARKET_EVENT)
                .symbol("AAPL")
                .timeframe("5m")
                .recordCount(2)
                .data(Collections.emptyList())
                .success(true)
                .build();
        
        when(dataSourceFactory.getDataSource(DataSourceType.CORE_MARKET_EVENT))
                .thenReturn(coreMarketEventDataSource);
        when(coreMarketEventDataSource.fetchData(anyString(), anyString(), any()))
                .thenReturn(mockResult);
        
        AIResponseDTO response = aiWorkflowService.executeWorkflow(testRequest);
        
        assertNotNull(response);
        assertEquals("dry_run", response.getStatus());
        assertNotNull(response.getOutput());
        assertTrue(response.getOutput().contains("SYSTEM INSTRUCTIONS"));
        assertTrue(response.getOutput().contains("MARKET DATA FOR ANALYSIS"));
        
        // Verify AI service was not called in dry run mode
        verify(aiClientService, never()).sendReasoningRequest(any());
    }
    
    @Test
    void testExecuteWorkflowWithPredefinedPrompt() throws Exception {
        // Mock data source result
        DataSourceResult mockResult = DataSourceResult.builder()
                .dataSourceType(DataSourceType.CORE_MARKET_EVENT)
                .symbol("AAPL")
                .timeframe("5m")
                .recordCount(2)
                .data(Collections.emptyList())
                .success(true)
                .build();
        
        when(dataSourceFactory.getDataSource(DataSourceType.CORE_MARKET_EVENT))
                .thenReturn(coreMarketEventDataSource);
        when(coreMarketEventDataSource.fetchData(anyString(), anyString(), any()))
                .thenReturn(mockResult);
        
        AIResponseDTO mockResponse = AIResponseDTO.builder()
                .requestId("test-123")
                .status("completed")
                .output("Analysis: AAPL shows bullish momentum")
                .model("gpt-4")
                .build();
        
        when(aiClientService.sendReasoningRequest(any()))
                .thenReturn(mockResponse);
        
        AIResponseDTO response = aiWorkflowService.executeWorkflow(testRequest);
        
        assertNotNull(response);
        assertEquals("completed", response.getStatus());
        assertNotNull(response.getOutput());
        
        verify(aiClientService, times(1)).sendReasoningRequest(any());
    }
    
    @Test
    void testExecuteWorkflowWithCustomPrompt() throws Exception {
        testRequest.setPromptType("CUSTOM");
        testRequest.setCustomPromptText("Analyze this stock for momentum trading");
        
        // Mock data source result
        DataSourceResult mockResult = DataSourceResult.builder()
                .dataSourceType(DataSourceType.CORE_MARKET_EVENT)
                .symbol("AAPL")
                .timeframe("5m")
                .recordCount(0)
                .data(Collections.emptyList())
                .success(true)
                .build();
        
        when(dataSourceFactory.getDataSource(DataSourceType.CORE_MARKET_EVENT))
                .thenReturn(coreMarketEventDataSource);
        when(coreMarketEventDataSource.fetchData(anyString(), anyString(), any()))
                .thenReturn(mockResult);
        
        AIResponseDTO mockResponse = AIResponseDTO.builder()
                .requestId("test-456")
                .status("completed")
                .output("Custom analysis complete")
                .model("gpt-4")
                .build();
        
        when(aiClientService.sendReasoningRequest(any()))
                .thenReturn(mockResponse);
        
        AIResponseDTO response = aiWorkflowService.executeWorkflow(testRequest);
        
        assertNotNull(response);
        assertEquals("completed", response.getStatus());
        
        verify(aiClientService, times(1)).sendReasoningRequest(any());
    }
}
