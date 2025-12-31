package com.trade.app.datasource.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Configuration for a specific data source within a timeframe.
 * 
 * Defines what data to fetch, time ranges, and any data source-specific settings.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceConfig {
    
    /**
     * Type of data source (e.g., CORE_MARKET_EVENT, OHLC).
     */
    private DataSourceType dataSourceType;
    
    /**
     * Whether this data source is enabled for this request.
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * Start time for data retrieval (inclusive).
     */
    private Instant fromTime;
    
    /**
     * End time for data retrieval (inclusive).
     */
    private Instant toTime;
    
    /**
     * Maximum number of records to retrieve.
     * If null or 0, no limit is applied.
     */
    private Integer maxRecords;
    
    /**
     * For OHLC data: whether to use external data source.
     * For other sources: custom flag for alternative data providers.
     */
    @Builder.Default
    private boolean useExternalSource = false;
    
    /**
     * Additional filters specific to the data source type.
     * Examples:
     * - For CORE_MARKET_EVENT: filter by indicator names
     * - For OHLC: filter by price range
     */
    private String filterCriteria;
}
