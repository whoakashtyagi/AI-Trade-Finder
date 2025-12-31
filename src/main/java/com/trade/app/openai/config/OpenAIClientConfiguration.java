package com.trade.app.openai.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.trade.app.config.properties.OpenAIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;

/**
 * Spring Configuration for OpenAI Client integration.
 * 
 * This configuration class initializes the OpenAI client using the official
 * openai-java library and configures retry templates for resilience.
 * 
 * @author AI Trade Finder Team
 */
@Configuration
@EnableConfigurationProperties(OpenAIProperties.class)
@RequiredArgsConstructor
@Slf4j
public class OpenAIClientConfiguration {
    
    private final OpenAIProperties openAIProperties;
    
    /**
     * Creates and configures the OpenAI client bean.
     * 
     * The client is configured with:
     * - API key from application properties
     * - Custom base URL if specified
     * - Organization ID if provided
     * - Timeout settings
     * 
     * @return Configured OpenAIClient instance
     * @throws IllegalStateException if API key is not configured
     */
    @Bean
    public OpenAIClient openAIClient() {
        if (openAIProperties.getApiKey() == null || openAIProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured. " +
                    "Please set 'openai.api-key' in application.properties");
        }
        
        log.info("Initializing OpenAI client with base URL: {}", openAIProperties.getBaseUrl());
        
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                .apiKey(openAIProperties.getApiKey())
                .baseUrl(openAIProperties.getBaseUrl())
                .timeout(Duration.ofSeconds(openAIProperties.getTimeoutSeconds()));
        
        // Add organization ID if provided
        if (openAIProperties.getOrgId() != null && !openAIProperties.getOrgId().isBlank()) {
            builder.organization(openAIProperties.getOrgId());
            log.info("OpenAI client configured with organization ID");
        }
        
        OpenAIClient client = builder.build();
        log.info("OpenAI client successfully initialized");
        
        return client;
    }
    
    /**
     * Creates a retry template for OpenAI API calls with exponential backoff.
     * 
     * Configured with:
     * - Maximum retry attempts from properties
     * - Exponential backoff starting at 1 second
     * - Maximum backoff of 10 seconds
     * - Multiplier of 2.0 for exponential growth
     * 
     * @return Configured RetryTemplate
     */
    @Bean(name = "openAIRetryTemplate")
    public RetryTemplate openAIRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(openAIProperties.getMaxRetries());
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L); // 1 second
        backOffPolicy.setMaxInterval(10000L); // 10 seconds
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        log.info("OpenAI retry template configured with {} max attempts", 
                openAIProperties.getMaxRetries());
        
        return retryTemplate;
    }
}
