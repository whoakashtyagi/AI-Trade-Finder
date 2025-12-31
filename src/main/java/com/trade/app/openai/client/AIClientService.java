package com.trade.app.openai.client;

import com.trade.app.openai.dto.AIRequestDTO;
import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.openai.exception.AIClientException;

/**
 * Generic service interface for AI client operations.
 * 
 * This interface defines the contract for interacting with AI services,
 * particularly for sending reasoning tasks and receiving structured responses.
 * 
 * Implementations should handle:
 * - Request serialization
 * - API communication
 * - Response deserialization
 * - Error handling and retries
 * 
 * @author AI Trade Finder Team
 */
public interface AIClientService {
    
    /**
     * Sends a reasoning request to the AI service and returns the structured response.
     * 
     * This method handles the complete lifecycle of an AI reasoning request:
     * 1. Validates and transforms the input request
     * 2. Sends the request to the AI provider
     * 3. Parses and validates the response
     * 4. Returns a structured DTO with the results
     * 
     * @param request The AI request containing the prompt and configuration
     * @return AIResponseDTO containing the AI's response with reasoning output
     * @throws AIClientException if the request fails, times out, or returns an error
     * @throws IllegalArgumentException if the request is null or invalid
     */
    AIResponseDTO sendReasoningRequest(AIRequestDTO request) throws AIClientException;
    
    /**
     * Sends a reasoning request with a custom model override.
     * 
     * This method allows specifying a different model than the default configured one.
     * Useful for A/B testing or using specialized models for specific tasks.
     * 
     * @param request The AI request containing the prompt and configuration
     * @param model The specific model to use (e.g., "o1", "o1-preview", "o1-mini")
     * @return AIResponseDTO containing the AI's response with reasoning output
     * @throws AIClientException if the request fails, times out, or returns an error
     * @throws IllegalArgumentException if the request or model is null/invalid
     */
    AIResponseDTO sendReasoningRequest(AIRequestDTO request, String model) throws AIClientException;
    
    /**
     * Checks if the AI client is properly configured and can connect to the API.
     * 
     * This method can be used for health checks and startup validation.
     * 
     * @return true if the client is healthy and can make requests, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Sends a reasoning request with structured output support.
     * 
     * This method uses OpenAI's Structured Outputs feature to return type-safe,
     * validated responses that conform to a predefined Java class schema.
     * The AI response will be automatically deserialized into the specified class.
     * 
     * Benefits:
     * - Type safety - no manual JSON parsing needed
     * - Automatic validation - responses conform to the schema
     * - Cleaner code - direct Java object access
     * - Reduced errors - eliminates parsing exceptions
     * 
     * @param <T> The type of structured output expected
     * @param request The AI request containing the prompt and configuration
     * @param model The specific model to use (e.g., "o1", "o1-preview", "o1-mini")
     * @param responseClass The class defining the structure of the expected response
     * @return An instance of the responseClass with data from the AI response
     * @throws AIClientException if the request fails, times out, or returns an error
     * @throws IllegalArgumentException if the request, model, or responseClass is null/invalid
     */
    <T> T sendStructuredRequest(AIRequestDTO request, String model, Class<T> responseClass) 
        throws AIClientException;
    
    /**
     * Sends a reasoning request with structured output using the default model.
     * 
     * This is a convenience method that uses the configured default model.
     * See {@link #sendStructuredRequest(AIRequestDTO, String, Class)} for details.
     * 
     * @param <T> The type of structured output expected
     * @param request The AI request containing the prompt and configuration
     * @param responseClass The class defining the structure of the expected response
     * @return An instance of the responseClass with data from the AI response
     * @throws AIClientException if the request fails, times out, or returns an error
     * @throws IllegalArgumentException if the request or responseClass is null/invalid
     */
    <T> T sendStructuredRequest(AIRequestDTO request, Class<T> responseClass) 
        throws AIClientException;
}
