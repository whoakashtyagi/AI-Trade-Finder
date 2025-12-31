package com.trade.app.datasource;

import com.trade.app.datasource.model.DataSourceConfig;
import com.trade.app.datasource.model.DataSourceResult;
import com.trade.app.datasource.model.DataSourceType;

/**
 * Generic interface for data sources in the AI workflow system.
 * 
 * This abstraction allows the system to work with different types of market data
 * in a uniform way. Each data source implementation handles fetching, formatting,
 * and preparing its specific data type for AI analysis.
 * 
 * Design Pattern: Strategy Pattern
 * - Allows runtime selection of data source implementation
 * - Enables easy addition of new data sources without modifying existing code
 * - Promotes loose coupling between data retrieval and business logic
 * 
 * SOLID Principles:
 * - Open/Closed: Open for extension (new implementations), closed for modification
 * - Dependency Inversion: Depend on abstraction, not concrete implementations
 * - Interface Segregation: Single, focused interface for data retrieval
 * 
 * @author AI Trade Finder Team
 */
public interface DataSource {
    
    /**
     * Fetches data from this data source based on the provided configuration.
     * 
     * @param symbol The trading symbol to fetch data for (e.g., "AAPL", "NQ")
     * @param timeframe The timeframe for the data (e.g., "5m", "1h", "1d")
     * @param config Configuration specifying time range, filters, and limits
     * @return DataSourceResult containing the fetched data and metadata
     */
    DataSourceResult fetchData(String symbol, String timeframe, DataSourceConfig config);
    
    /**
     * Returns the type of data source this implementation handles.
     * 
     * @return DataSourceType enum value
     */
    DataSourceType getDataSourceType();
    
    /**
     * Checks if this data source supports the given symbol.
     * 
     * @param symbol The trading symbol to check
     * @return true if the symbol is supported, false otherwise
     */
    boolean supportsSymbol(String symbol);
    
    /**
     * Checks if this data source supports the given timeframe.
     * 
     * @param timeframe The timeframe to check (e.g., "5m", "1h")
     * @return true if the timeframe is supported, false otherwise
     */
    boolean supportsTimeframe(String timeframe);
    
    /**
     * Provides a description of this data source for documentation/UI purposes.
     * 
     * @return Human-readable description of the data source
     */
    String getDescription();
    
    /**
     * Checks if this data source is currently available and healthy.
     * Can be used for health checks and circuit breaker patterns.
     * 
     * @return true if the data source is operational, false otherwise
     */
    default boolean isHealthy() {
        return true;
    }
}
