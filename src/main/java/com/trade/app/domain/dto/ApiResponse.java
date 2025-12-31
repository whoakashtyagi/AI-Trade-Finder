package com.trade.app.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trade.app.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper for successful operations.
 * 
 * Provides a consistent response structure across all endpoints,
 * including status, message, timestamp, and optional data payload.
 * 
 * @param <T> The type of data payload
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * Response status (success, error, warning).
     */
    @Builder.Default
    private String status = Constants.Api.SUCCESS_STATUS;

    /**
     * Human-readable message describing the response.
     */
    private String message;
    
    /**
     * Response timestamp.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Optional data payload.
     */
    private T data;
    
    /**
     * Optional metadata (e.g., pagination info, processing time).
     */
    private Object metadata;
    
    /**
     * Creates a success response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(Constants.Api.SUCCESS_STATUS)
                .data(data)
                .build();
    }
    
    /**
     * Creates a success response with message and data.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(Constants.Api.SUCCESS_STATUS)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * Creates a success response with message only.
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(Constants.Api.SUCCESS_STATUS)
                .message(message)
                .build();
    }
}
