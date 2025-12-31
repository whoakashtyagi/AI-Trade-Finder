package com.trade.app.openai.mapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.trade.app.util.Constants;
import org.springframework.stereotype.Component;

import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.trade.app.openai.dto.AIRequestDTO;
import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.openai.exception.AIResponseParsingException;

import lombok.extern.slf4j.Slf4j;

/**
 * Mapper utility for OpenAI SDK types using the Responses API.
 * 
 * @author AI Trade Finder Team
 */
@Component
@Slf4j
public class OpenAIMapper {
    
    /**
     * Converts an AIRequestDTO to OpenAI ResponseCreateParams.
     * 
     * @param request The application-level request DTO
     * @param model The model to use for the request
     * @return ResponseCreateParams configured for the OpenAI SDK
     * @throws IllegalArgumentException if request is invalid
     */
    public ResponseCreateParams toOpenAIRequest(AIRequestDTO request, String model) {
        if (request == null || !request.isValid()) {
            throw new IllegalArgumentException("Invalid request: input is required");
        }
        
        log.debug("Mapping request to OpenAI parameters for model: {}", model);
        
        // Build params using Response API
        ResponseCreateParams.Builder builder = ResponseCreateParams.builder()
                .input(request.getInput())
                .model(model);
        
        // Add system instructions if present
        if (request.getSystemInstructions() != null && !request.getSystemInstructions().isBlank()) {
            builder.instructions(request.getSystemInstructions());
        }
        
        // Add optional parameters if present
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        
        if (request.getMaxTokens() != null) {
            builder.maxOutputTokens(request.getMaxTokens());
        }
        
        // Add conversation state support
        if (request.getPreviousResponseId() != null && !request.getPreviousResponseId().isBlank()) {
            builder.previousResponseId(request.getPreviousResponseId());
            log.debug("Using previous response ID for conversation continuity: {}", 
                    request.getPreviousResponseId());
        }
        
        // Set store parameter (default true)
        if (request.getStore() != null) {
            builder.store(request.getStore());
        }
        
        log.debug("Successfully mapped request to OpenAI parameters");
        return builder.build();
    }
    
    /**
     * Converts an OpenAI Response to AIResponseDTO.
     * Extracts text from response.output() structure.
     * 
     * @param response The Response from OpenAI SDK
     * @param requestId The original request ID for correlation
     * @param startTime The time when the request was sent
     * @return AIResponseDTO with parsed response data
     * @throws AIResponseParsingException if the response cannot be parsed
     */
    public AIResponseDTO toAIResponse(Response response, String requestId, long startTime) 
            throws AIResponseParsingException {
        
        if (response == null) {
            throw new AIResponseParsingException("Received null response from OpenAI");
        }
        
        try {
            log.debug("Mapping OpenAI response to DTO. Response ID: {}", response.id());
            
            // Extract output text from the response structure
            String output = extractOutputText(response);
            
            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Extract usage statistics
            AIResponseDTO.Usage usage = null;
            if (response.usage().isPresent()) {
                var usageData = response.usage().get();
                usage = AIResponseDTO.Usage.builder()
                        .promptTokens((int) usageData.inputTokens())
                        .completionTokens((int) usageData.outputTokens())
                        .totalTokens((int) usageData.totalTokens())
                        .build();
            }
            
            // Extract metadata
            Map<String, Object> metadata = new HashMap<>();
            if (response.metadata().isPresent()) {
                metadata.put("metadata", response.metadata().get().toString());
            }
            
            // Build response DTO
            AIResponseDTO dto = AIResponseDTO.builder()
                    .id(response.id())
                    .model(response.model().toString())
                    .output(output)
                    .createdAt(Instant.ofEpochSecond((long) response.createdAt()))
                    .status(response.status().toString())
                    .requestId(requestId)
                    .processingTimeMs(processingTime)
                    .usage(usage)
                    .metadata(metadata)
                    .object("response")
                    .previousResponseId(response.previousResponseId().orElse(null))
                    .stored(true)
                    .serviceTier(response.serviceTier().map(tier -> tier.toString()).orElse(null))
                    .build();
            
            // Check for errors
            if (response.error().isPresent()) {
                var error = response.error().get();
                AIResponseDTO.ErrorDetails errorDetails = AIResponseDTO.ErrorDetails.builder()
                        .message(error.message())
                        .type(error.code() != null ? error.code().toString() : "unknown")
                        .code(error.code() != null ? error.code().toString() : null)
                        .build();
                dto.setError(errorDetails);
                dto.setStatus(Constants.AIResponseStatus.FAILED);
            }
            
            log.debug("Successfully mapped OpenAI response. Response ID: {}, Status: {}", 
                    dto.getId(), dto.getStatus());
            
            return dto;
            
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new AIResponseParsingException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts output text from Response.
     * Response.output() returns List<ResponseOutputItem>
     * Each item can be a message, tool call, etc.
     * We need to extract text from message content.
     */
    private String extractOutputText(Response response) {
        try {
            if (response.output() == null || response.output().isEmpty()) {
                log.warn("No output items in response");
                return "Response received but no output available";
            }
            
            StringBuilder outputText = new StringBuilder();
            
            for (ResponseOutputItem item : response.output()) {
                // Check if this is a message
                if (item.message().isPresent()) {
                    ResponseOutputMessage message = item.message().get();
                    
                    // Extract text from message content
                    for (ResponseOutputMessage.Content content : message.content()) {
                        if (content.outputText().isPresent()) {
                            ResponseOutputText textContent = content.outputText().get();
                            outputText.append(textContent.text()).append("\n");
                        }
                    }
                }
            }
            
            String result = outputText.toString().trim();
            return result.isEmpty() ? "Response received but no text content available" : result;
            
        } catch (Exception e) {
            log.error("Failed to extract output text from response", e);
            return "Response received but content extraction failed: " + e.getMessage();
        }
    }
    
    /**
     * Extracts error information from OpenAI SDK exceptions.
     */
    public Map<String, Object> extractErrorDetails(Exception exception) {
        Map<String, Object> errorDetails = new HashMap<>();
        
        errorDetails.put("type", exception.getClass().getSimpleName());
        errorDetails.put("message", exception.getMessage());
        
        // Try to extract HTTP status if available
        String message = exception.getMessage();
        if (message != null) {
            if (message.contains("401") || message.contains("Unauthorized")) {
                errorDetails.put("statusCode", 401);
                errorDetails.put("errorCode", "authentication_error");
            } else if (message.contains("429") || message.contains("rate limit")) {
                errorDetails.put("statusCode", 429);
                errorDetails.put("errorCode", "rate_limit_exceeded");
            } else if (message.contains("500") || message.contains("503")) {
                errorDetails.put("statusCode", 500);
                errorDetails.put("errorCode", "server_error");
            } else if (message.contains("timeout")) {
                errorDetails.put("statusCode", 408);
                errorDetails.put("errorCode", "timeout");
            }
        }
        
        return errorDetails;
    }
    
    /**
     * Converts an AIRequestDTO to OpenAI StructuredResponseCreateParams.
     */
    public <T> StructuredResponseCreateParams<T> toStructuredOpenAIRequest(
            AIRequestDTO request, String model, Class<T> responseClass) {
        
        if (request == null || !request.isValid()) {
            throw new IllegalArgumentException("Invalid request: input is required");
        }
        
        if (responseClass == null) {
            throw new IllegalArgumentException("Response class cannot be null");
        }
        
        log.debug("Mapping request to structured OpenAI parameters for model: {}, response type: {}", 
                model, responseClass.getSimpleName());
        
        // Build params using Response API with structured outputs
        StructuredResponseCreateParams.Builder<T> builder = ResponseCreateParams.builder()
                .input(request.getInput())
                .model(model)
                .text(responseClass);
        
        // Add system instructions if present
        if (request.getSystemInstructions() != null && !request.getSystemInstructions().isBlank()) {
            builder.instructions(request.getSystemInstructions());
        }
        
        // Add optional parameters if present
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        
        if (request.getMaxTokens() != null) {
            builder.maxOutputTokens(request.getMaxTokens());
        }
        
        // Add conversation state support
        if (request.getPreviousResponseId() != null && !request.getPreviousResponseId().isBlank()) {
            builder.previousResponseId(request.getPreviousResponseId());
            log.debug("Using previous response ID for conversation continuity: {}", 
                    request.getPreviousResponseId());
        }
        
        // Set store parameter (default true)
        if (request.getStore() != null) {
            builder.store(request.getStore());
        }
        
        log.debug("Successfully mapped request to structured OpenAI parameters");
        
        return builder.build();
    }
    
    /**
     * Extracts structured output from an OpenAI StructuredResponse.
     * For StructuredResponse, we need to extract from output items.
     */
    public <T> T extractStructuredOutput(StructuredResponse<T> response, Class<T> responseClass) 
            throws AIResponseParsingException {
        
        if (response == null) {
            throw new AIResponseParsingException("Received null response from OpenAI");
        }
        
        if (responseClass == null) {
            throw new AIResponseParsingException("Response class cannot be null");
        }
        
        try {
            log.debug("Extracting structured output from response. Response ID: {}, Type: {}", 
                    response.id(), responseClass.getSimpleName());
            
            // Check for errors first
            if (response.error().isPresent()) {
                var error = response.error().get();
                throw new AIResponseParsingException(
                        String.format("Response contains error: %s (code: %s)", 
                                error.message(), 
                                error.code() != null ? error.code().toString() : "unknown"));
            }
            
            // Check if response has output
            if (response.output() == null || response.output().isEmpty()) {
                throw new AIResponseParsingException("Response contains no output data");
            }
            
            // Extract structured data from response output items
            // The structured output should be in the message content
            for (var item : response.output()) {
                if (item.message().isPresent()) {
                    var message = item.message().get();
                    for (var content : message.content()) {
                        if (content.outputText().isPresent()) {
                            T result = content.outputText().get();
                            
                            if (result == null) {
                                throw new AIResponseParsingException(
                                        "Structured output extraction returned null");
                            }
                            
                            log.debug("Successfully extracted structured output. Type: {}", 
                                    responseClass.getSimpleName());
                            
                            return result;
                        }
                    }
                }
            }
            
            throw new AIResponseParsingException("No structured output found in response");
            
        } catch (AIResponseParsingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract structured output from response", e);
            throw new AIResponseParsingException(
                    "Failed to extract structured output: " + e.getMessage(), e);
        }
    }
}