package com.trade.app.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for Jackson ObjectMapper.
 * 
 * Provides a centrally configured ObjectMapper bean with sensible defaults
 * for JSON serialization and deserialization across the application.
 * 
 * @author AI Trade Finder Team
 */
@Configuration
public class JacksonConfig {
    
    /**
     * Configures and provides the application-wide ObjectMapper bean.
     * 
     * Configuration includes:
     * - JavaTimeModule for Java 8 date/time types (Instant, LocalDateTime, etc.)
     * - Pretty printing disabled for production efficiency
     * - Lenient deserialization (ignores unknown properties)
     * - Proper handling of Java 8 date/time types
     * 
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
