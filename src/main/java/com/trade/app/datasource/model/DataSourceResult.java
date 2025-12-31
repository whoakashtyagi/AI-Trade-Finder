package com.trade.app.datasource.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Result returned by a data source after fetching data.
 * 
 * Contains the fetched data in a generic format along with metadata
 * about the data source and retrieval operation.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceResult {
    
    /**
     * Type of data source that produced this result.
     */
    private DataSourceType dataSourceType;
    
    /**
     * Symbol that was queried.
     */
    private String symbol;
    
    /**
     * Timeframe that was queried.
     */
    private String timeframe;
    
    /**
     * Number of records retrieved.
     */
    private int recordCount;
    
    /**
     * The actual data records in a generic map format.
     * Each map represents one record with field names as keys.
     */
    private List<Map<String, Object>> data;
    
    /**
     * Additional metadata about the data retrieval.
     * Can include information like:
     * - Data source name/version
     * - Query execution time
     * - Cache hit/miss status
     * - Data quality indicators
     */
    private Map<String, Object> metadata;
    
    /**
     * Error message if data retrieval failed.
     * Null if successful.
     */
    private String errorMessage;
    
    /**
     * Whether the data retrieval was successful.
     */
    @Builder.Default
    private boolean success = true;
}
