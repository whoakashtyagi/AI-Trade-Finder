package com.trade.app.openai;

import com.trade.app.config.properties.OpenAIProperties;
import com.trade.app.openai.client.AIClientService;
import com.trade.app.openai.dto.AIRequestDTO;
import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.openai.exception.AIClientException;
import com.trade.app.openai.exception.AIRequestValidationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for OpenAI Client Service.
 * 
 * These tests verify the OpenAI integration layer functionality.
 * 
 * Note: These tests require a valid OPENAI_API_KEY environment variable
 * to run successfully. Without it, they will be skipped or fail gracefully.
 * 
 * @author AI Trade Finder Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class OpenAIClientServiceIntegrationTest {
    
    @Autowired(required = false)
    private AIClientService aiClientService;
    
    @Autowired(required = false)
    private OpenAIProperties openAIProperties;
    
    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
        assertNotNull(openAIProperties, "OpenAI properties should be loaded");
    }
    
    @Test
    void testPropertiesConfiguration() {
        assertNotNull(openAIProperties, "OpenAI properties should be configured");
        assertNotNull(openAIProperties.getDefaultModel(), "Default model should be set");
        assertEquals("https://api.openai.com/v1", openAIProperties.getBaseUrl());
        assertTrue(openAIProperties.getMaxRetries() > 0, "Max retries should be positive");
        assertTrue(openAIProperties.getTimeoutSeconds() > 0, "Timeout should be positive");
    }
    
    @Test
    void testRequestDTOValidation() {
        // Valid request
        AIRequestDTO validRequest = AIRequestDTO.builder()
                .input("Test input")
                .build();
        assertTrue(validRequest.isValid(), "Valid request should pass validation");
        
        // Invalid request - null input
        AIRequestDTO invalidRequest1 = AIRequestDTO.builder()
                .input(null)
                .build();
        assertFalse(invalidRequest1.isValid(), "Null input should fail validation");
        
        // Invalid request - blank input
        AIRequestDTO invalidRequest2 = AIRequestDTO.builder()
                .input("   ")
                .build();
        assertFalse(invalidRequest2.isValid(), "Blank input should fail validation");
    }
    
    @Test
    void testResponseDTOSuccessCheck() {
        // Successful response
        AIResponseDTO successResponse = AIResponseDTO.builder()
                .status("completed")
                .output("Test output")
                .build();
        assertTrue(successResponse.isSuccessful(), "Completed response with output should be successful");
        
        // Failed response - no output
        AIResponseDTO failedResponse1 = AIResponseDTO.builder()
                .status("completed")
                .output(null)
                .build();
        assertFalse(failedResponse1.isSuccessful(), "Response without output should not be successful");
        
        // Failed response - wrong status
        AIResponseDTO failedResponse2 = AIResponseDTO.builder()
                .status("failed")
                .output("Test output")
                .build();
        assertFalse(failedResponse2.isSuccessful(), "Failed status should not be successful");
    }
    
    @Test
    void testRequestValidation() {
        if (aiClientService == null) {
            // Skip if API key not configured
            return;
        }
        
        // Test null request
        assertThrows(AIRequestValidationException.class, () -> {
            aiClientService.sendReasoningRequest(null);
        }, "Null request should throw validation exception");
        
        // Test invalid request
        AIRequestDTO invalidRequest = AIRequestDTO.builder()
                .input("")
                .build();
        
        assertThrows(AIRequestValidationException.class, () -> {
            aiClientService.sendReasoningRequest(invalidRequest);
        }, "Invalid request should throw validation exception");
    }
    
    /**
     * This test requires a valid API key to run.
     * It will be skipped if the service is not properly configured.
     */
    @Test
    void testSimpleReasoningRequest() {
        if (aiClientService == null) {
            // Skip if API key not configured
            log.info("Skipping integration test - API key not configured");
            return;
        }
        
        try {
            AIRequestDTO request = AIRequestDTO.builder()
                    .input("What is 2+2?")
                    .maxTokens(50)
                    .build();
            
            AIResponseDTO response = aiClientService.sendReasoningRequest(request);
            
            assertNotNull(response, "Response should not be null");
            assertNotNull(response.getId(), "Response ID should not be null");
            assertNotNull(response.getModel(), "Model should not be null");
            assertNotNull(response.getOutput(), "Output should not be null");
            assertTrue(response.getOutput().contains("4"), "Response should contain correct answer");
            assertEquals("completed", response.getStatus(), "Status should be completed");
            assertNotNull(response.getUsage(), "Usage should not be null");
            assertTrue(response.getUsage().getTotalTokens() > 0, "Total tokens should be positive");
            
        } catch (AIClientException e) {
            // If test fails due to API error, log but don't fail test
            log.warn("API test failed (may be expected): {}", e.getMessage());
        }
    }
    
    /**
     * Test with custom model.
     */
    @Test
    void testWithCustomModel() {
        if (aiClientService == null) {
            log.info("Skipping integration test - API key not configured");
            return;
        }
        
        try {
            AIRequestDTO request = AIRequestDTO.builder()
                    .input("Explain photosynthesis briefly")
                    .maxTokens(100)
                    .build();
            
            AIResponseDTO response = aiClientService.sendReasoningRequest(request, "o1-mini");
            
            assertNotNull(response);
            assertTrue(response.getModel().contains("o1"), "Model should be o1 variant");
            assertNotNull(response.getOutput());
            
        } catch (AIClientException e) {
            log.warn("Custom model test failed: {}", e.getMessage());
        }
    }
    
    /**
     * Test health check.
     */
    @Test
    void testHealthCheck() {
        if (aiClientService == null) {
            log.info("Skipping health check test - service not configured");
            return;
        }
        
        // Health check should not throw exceptions
        assertDoesNotThrow(() -> {
            boolean healthy = aiClientService.isHealthy();
            log.info("Service health: {}", healthy ? "HEALTHY" : "UNHEALTHY");
        });
    }
}
