package com.trade.app.openai.impl;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.trade.app.config.properties.OpenAIProperties;
import com.trade.app.openai.client.AIClientService;
import com.trade.app.openai.dto.AIRequestDTO;
import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.openai.exception.AIClientException;
import com.trade.app.openai.exception.AIRequestValidationException;
import com.trade.app.openai.mapper.OpenAIMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Implementation of AIClientService using the official OpenAI Java SDK.
 * 
 * This service provides integration with OpenAI's Chat Completions API
 * (including reasoning models like o1) using the openai-java library.
 * 
 * Features:
 * - Automatic retries with exponential backoff
 * - Request/response mapping and validation
 * - Comprehensive error handling
 * - Request correlation via request IDs
 * - Optional logging of requests/responses
 * 
 * @author AI Trade Finder Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIClientServiceImpl implements AIClientService {
    
    private final OpenAIClient openAIClient;
    private final OpenAIMapper openAIMapper;
    private final OpenAIProperties openAIProperties;
    private final RetryTemplate openAIRetryTemplate;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public AIResponseDTO sendReasoningRequest(AIRequestDTO request) throws AIClientException {
        return sendReasoningRequest(request, openAIProperties.getDefaultModel());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public AIResponseDTO sendReasoningRequest(AIRequestDTO request, String model) throws AIClientException {
        // Validate inputs
        if (request == null) {
            throw new AIRequestValidationException("Request cannot be null");
        }
        
        if (!request.isValid()) {
            throw new AIRequestValidationException("Request input is required and cannot be blank");
        }
        
        if (model == null || model.isBlank()) {
            throw new AIRequestValidationException("Model cannot be null or blank");
        }
        
        // Generate request ID if not provided
        String requestId = request.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            request.setRequestId(requestId);
        }
        
        log.info("Sending reasoning request to OpenAI. Request ID: {}, Model: {}", requestId, model);
        
        // Log conversation continuity if using previous response
        if (request.getPreviousResponseId() != null) {
            log.info("Continuing conversation with previous response ID: {}", request.getPreviousResponseId());
        }
        
        if (openAIProperties.getLoggingEnabled()) {
            log.debug("Request input: {}", request.getInput());
            if (request.getMetadata() != null) {
                log.debug("Request metadata: {}", request.getMetadata());
            }
        }
        
        final long startTime = System.currentTimeMillis();
        final String finalRequestId = requestId;
        
        try {
            // Map request to OpenAI SDK format
            ResponseCreateParams params = openAIMapper.toOpenAIRequest(request, model);
            
            // Execute with retry template
            AIResponseDTO response = openAIRetryTemplate.execute(context -> {
                log.debug("Executing OpenAI request (attempt {})", context.getRetryCount() + 1);
                
                try {
                    // Call OpenAI API using simple Responses API
                    Response openAIResponse = openAIClient.responses().create(params);
                    
                    // Map response to DTO
                    AIResponseDTO responseDTO = openAIMapper.toAIResponse(openAIResponse, finalRequestId, startTime);
                    
                    log.info("Successfully received response from OpenAI. Request ID: {}, Processing time: {}ms",
                            finalRequestId, responseDTO.getProcessingTimeMs());
                    
                    if (openAIProperties.getLoggingEnabled() && responseDTO.getOutput() != null) {
                        log.debug("Response output: {}", responseDTO.getOutput().substring(
                                0, Math.min(200, responseDTO.getOutput().length())) + "...");
                    }
                    
                    return responseDTO;
                    
                } catch (Exception e) {
                    log.warn("OpenAI request failed (attempt {}): {}", 
                            context.getRetryCount() + 1, e.getMessage());
                    
                    // Extract error details
                    Map<String, Object> errorDetails = openAIMapper.extractErrorDetails(e);
                    Integer statusCode = (Integer) errorDetails.get("statusCode");
                    String errorCode = (String) errorDetails.get("errorCode");
                    
                    // Create exception with details
                    AIClientException exception = new AIClientException(
                            "OpenAI API request failed: " + e.getMessage(),
                            e,
                            statusCode,
                            errorCode,
                            finalRequestId
                    );
                    
                    // Determine if error is retryable
                    if (!exception.isRetryable() || context.getRetryCount() >= openAIProperties.getMaxRetries() - 1) {
                        log.error("OpenAI request failed permanently. Request ID: {}", finalRequestId, e);
                    }
                    
                    throw exception;
                }
            });
            
            return response;
            
        } catch (AIClientException e) {
            // Already an AIClientException (includes AIResponseParsingException), just rethrow
            throw e;
        } catch (Exception e) {
            // Unexpected exception
            log.error("Unexpected error during OpenAI request. Request ID: {}", requestId, e);
            throw new AIClientException(
                    "Unexpected error during OpenAI request: " + e.getMessage(),
                    e,
                    null,
                    "unexpected_error",
                    requestId
            );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHealthy() {
        try {
            // Create a simple health check request
            AIRequestDTO healthCheck = AIRequestDTO.builder()
                    .input("ping")
                    .maxTokens(10)
                    .requestId("health-check-" + UUID.randomUUID())
                    .build();
            
            // Try to send a minimal request
            AIResponseDTO response = sendReasoningRequest(healthCheck);
            
            boolean healthy = response != null && response.isSuccessful();
            log.info("OpenAI client health check: {}", healthy ? "HEALTHY" : "UNHEALTHY");
            
            return healthy;
            
        } catch (Exception e) {
            log.warn("OpenAI client health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T sendStructuredRequest(AIRequestDTO request, Class<T> responseClass) 
            throws AIClientException {
        return sendStructuredRequest(request, openAIProperties.getDefaultModel(), responseClass);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T sendStructuredRequest(AIRequestDTO request, String model, Class<T> responseClass) 
            throws AIClientException {
        
        // Validate inputs
        if (request == null) {
            throw new AIRequestValidationException("Request cannot be null");
        }
        
        if (!request.isValid()) {
            throw new AIRequestValidationException("Request input is required and cannot be blank");
        }
        
        if (model == null || model.isBlank()) {
            throw new AIRequestValidationException("Model cannot be null or blank");
        }
        
        if (responseClass == null) {
            throw new AIRequestValidationException("Response class cannot be null");
        }
        
        // Generate request ID if not provided
        String requestId = request.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            request.setRequestId(requestId);
        }
        
        log.info("Sending structured reasoning request to OpenAI. Request ID: {}, Model: {}, Response Type: {}", 
                requestId, model, responseClass.getSimpleName());
        
        // Log conversation continuity if using previous response
        if (request.getPreviousResponseId() != null) {
            log.info("Continuing conversation with previous response ID: {}", request.getPreviousResponseId());
        }
        
        if (openAIProperties.getLoggingEnabled()) {
            log.debug("Request input: {}", request.getInput());
            log.debug("Expected structured output type: {}", responseClass.getName());
        }
        
        final long startTime = System.currentTimeMillis();
        final String finalRequestId = requestId;
        
        try {
            // Map request to OpenAI SDK format with structured output
            StructuredResponseCreateParams<T> params = openAIMapper.toStructuredOpenAIRequest(
                    request, model, responseClass);
            
            // Execute with retry template
            T structuredResponse = openAIRetryTemplate.execute(context -> {
                log.debug("Executing structured OpenAI request (attempt {})", context.getRetryCount() + 1);
                
                try {
                    // Call OpenAI API using Responses API with structured outputs
                    StructuredResponse<T> openAIResponse = openAIClient.responses().create(params);
                    
                    // Extract structured data from response
                    T result = openAIMapper.extractStructuredOutput(openAIResponse, responseClass);
                    
                    long processingTime = System.currentTimeMillis() - startTime;
                    log.info("Successfully received structured response from OpenAI. Request ID: {}, Processing time: {}ms, Type: {}",
                            finalRequestId, processingTime, responseClass.getSimpleName());
                    
                    if (openAIProperties.getLoggingEnabled()) {
                        log.debug("Structured response data: {}", result);
                    }
                    
                    return result;
                    
                } catch (Exception e) {
                    log.warn("Structured OpenAI request failed (attempt {}): {}", 
                            context.getRetryCount() + 1, e.getMessage());
                    
                    // Extract error details
                    Map<String, Object> errorDetails = openAIMapper.extractErrorDetails(e);
                    Integer statusCode = (Integer) errorDetails.get("statusCode");
                    String errorCode = (String) errorDetails.get("errorCode");
                    
                    // Create exception with details
                    AIClientException exception = new AIClientException(
                            "Structured OpenAI API request failed: " + e.getMessage(),
                            e,
                            statusCode,
                            errorCode,
                            finalRequestId
                    );
                    
                    // Determine if error is retryable
                    if (!exception.isRetryable() || context.getRetryCount() >= openAIProperties.getMaxRetries() - 1) {
                        log.error("Structured OpenAI request failed permanently. Request ID: {}", finalRequestId, e);
                    }
                    
                    throw exception;
                }
            });
            
            return structuredResponse;
            
        } catch (AIClientException e) {
            // Already an AIClientException, just rethrow
            throw e;
        } catch (Exception e) {
            // Unexpected exception
            log.error("Unexpected error during structured OpenAI request. Request ID: {}", requestId, e);
            throw new AIClientException(
                    "Unexpected error during structured OpenAI request: " + e.getMessage(),
                    e,
                    null,
                    "unexpected_error",
                    requestId
            );
        }
    }
}
