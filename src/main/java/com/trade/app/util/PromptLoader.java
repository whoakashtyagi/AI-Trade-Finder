package com.trade.app.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Utility for loading prompt files from resources.
 * 
 * This component provides methods to load system prompts and other
 * text resources from the classpath for use in AI interactions.
 * 
 * @author AI Trade Finder Team
 */
@Component
@Slf4j
public class PromptLoader {

    /**
     * Loads a prompt file from the classpath resources.
     * 
     * @param resourcePath Path to the resource file (e.g., "prompts/system_prompt.txt")
     * @return Content of the file as a string
     * @throws IOException if file cannot be read
     */
    public String loadPrompt(String resourcePath) throws IOException {
        log.debug("Loading prompt from: {}", resourcePath);
        
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            
            if (!resource.exists()) {
                log.warn("Prompt file not found: {}", resourcePath);
                return getDefaultPrompt();
            }
            
            try (InputStream inputStream = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                
                String content = reader.lines()
                    .collect(Collectors.joining("\n"));
                
                log.debug("Successfully loaded prompt: {} ({} chars)", resourcePath, content.length());
                return content;
            }
            
        } catch (IOException e) {
            log.error("Error loading prompt from {}: {}", resourcePath, e.getMessage());
            throw e;
        }
    }

    /**
     * Loads a prompt file with fallback to default.
     * 
     * @param resourcePath Path to the resource file
     * @param defaultPrompt Default prompt to use if file cannot be loaded
     * @return Content of the file or default prompt
     */
    public String loadPromptWithFallback(String resourcePath, String defaultPrompt) {
        try {
            return loadPrompt(resourcePath);
        } catch (IOException e) {
            log.warn("Failed to load prompt from {}, using default", resourcePath);
            return defaultPrompt;
        }
    }

    /**
     * Gets a default system prompt for trade finding.
     * 
     * @return Default system prompt
     */
    private String getDefaultPrompt() {
        return "You are an expert trading AI analyzing market structure. " +
               "Identify high-confidence trade setups based on liquidity sweeps, CISD patterns, and FVG entries. " +
               "Return your analysis as JSON with status, direction, confidence, entry, stop, targets, and narrative. " +
               "Use Smart Money Concepts to evaluate confluence and provide detailed reasoning for each trade setup.";
    }
}
