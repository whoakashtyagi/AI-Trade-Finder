package com.trade.app.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for OpenAI API integration.
 * 
 * This class binds properties from application.properties with the prefix 'openai'
 * to configure the OpenAI client.
 * 
 * @author AI Trade Finder Team
 */
@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
public class OpenAIProperties {
    
    /**
     * OpenAI API key for authentication.
     * Required for all API requests.
     */
    private String apiKey;
    
    /**
     * Base URL for OpenAI API.
     * Defaults to https://api.openai.com/v1 if not specified.
     */
    private String baseUrl = "https://api.openai.com/v1";
    
    /**
     * Optional organization ID for OpenAI requests.
     * Used when your API key is associated with multiple organizations.
     */
    private String orgId;
    
    /**
     * Default model to use for reasoning tasks.
     * Common values: "o1", "o1-preview", "o1-mini"
     */
    private String defaultModel = "o1";
    
    /**
     * Request timeout in seconds.
     * Default is 60 seconds.
     */
    private Integer timeoutSeconds = 60;
    
    /**
     * Maximum number of retry attempts for failed requests.
     * Default is 3.
     */
    private Integer maxRetries = 3;
    
    /**
     * Enable or disable request/response logging.
     * Default is false for security.
     */
    private Boolean loggingEnabled = false;
}
