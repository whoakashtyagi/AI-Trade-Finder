package com.trade.app.datasource.factory;

import com.trade.app.datasource.DataSource;
import com.trade.app.datasource.model.DataSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory and registry for managing data source implementations.
 * 
 * This component:
 * - Automatically discovers all DataSource implementations via Spring dependency injection
 * - Provides type-safe access to data sources by type
 * - Maintains a registry of available data sources
 * - Enables runtime discovery of data source capabilities
 * 
 * Design Pattern: Factory + Registry Pattern
 * - Factory: Creates/retrieves appropriate data source implementations
 * - Registry: Maintains catalog of available data sources
 * 
 * Benefits:
 * - New data sources are automatically registered (Spring auto-wiring)
 * - Centralized access point for all data sources
 * - Easy to test and mock
 * - Supports runtime discovery of capabilities
 * 
 * @author AI Trade Finder Team
 */
@Component
@Slf4j
public class DataSourceFactory {
    
    private final Map<DataSourceType, DataSource> dataSourceRegistry;
    
    /**
     * Constructor that auto-wires all DataSource implementations.
     * Spring will inject all beans implementing the DataSource interface.
     * 
     * @param dataSources List of all DataSource implementations from Spring context
     */
    public DataSourceFactory(List<DataSource> dataSources) {
        this.dataSourceRegistry = new EnumMap<>(DataSourceType.class);
        
        // Register all data sources by their type
        for (DataSource dataSource : dataSources) {
            DataSourceType type = dataSource.getDataSourceType();
            
            if (dataSourceRegistry.containsKey(type)) {
                log.warn("Multiple implementations found for DataSourceType: {}. Using: {}", 
                        type, dataSource.getClass().getSimpleName());
            }
            
            dataSourceRegistry.put(type, dataSource);
            log.info("Registered data source: {} -> {}", 
                    type.getDisplayName(), dataSource.getClass().getSimpleName());
        }
        
        log.info("DataSourceFactory initialized with {} data sources", dataSourceRegistry.size());
    }
    
    /**
     * Gets a data source implementation by type.
     * 
     * @param type The type of data source to retrieve
     * @return DataSource implementation
     * @throws IllegalArgumentException if no implementation exists for the type
     */
    public DataSource getDataSource(DataSourceType type) {
        DataSource dataSource = dataSourceRegistry.get(type);
        
        if (dataSource == null) {
            throw new IllegalArgumentException(
                    "No data source implementation registered for type: " + type.getDisplayName());
        }
        
        return dataSource;
    }
    
    /**
     * Gets a data source implementation by type code string.
     * 
     * @param typeCode The type code (e.g., "ohlc", "core_market_event")
     * @return DataSource implementation
     * @throws IllegalArgumentException if type code is invalid or no implementation exists
     */
    public DataSource getDataSource(String typeCode) {
        DataSourceType type = DataSourceType.fromCode(typeCode);
        return getDataSource(type);
    }
    
    /**
     * Checks if a data source implementation is registered for the given type.
     * 
     * @param type The data source type to check
     * @return true if an implementation is available, false otherwise
     */
    public boolean isAvailable(DataSourceType type) {
        return dataSourceRegistry.containsKey(type);
    }
    
    /**
     * Gets all available data source types.
     * 
     * @return Set of all registered data source types
     */
    public Set<DataSourceType> getAvailableTypes() {
        return dataSourceRegistry.keySet();
    }
    
    /**
     * Gets all registered data sources.
     * 
     * @return Collection of all data source implementations
     */
    public Collection<DataSource> getAllDataSources() {
        return dataSourceRegistry.values();
    }
    
    /**
     * Gets information about all available data sources.
     * Useful for API endpoints that list available data sources.
     * 
     * @return Map of data source type code to description
     */
    public Map<String, String> getAvailableDataSourcesInfo() {
        return dataSourceRegistry.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getCode(),
                        entry -> entry.getValue().getDescription(),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }
    
    /**
     * Gets health status of all data sources.
     * 
     * @return Map of data source type to health status
     */
    public Map<DataSourceType, Boolean> getHealthStatus() {
        Map<DataSourceType, Boolean> healthStatus = new LinkedHashMap<>();
        
        for (Map.Entry<DataSourceType, DataSource> entry : dataSourceRegistry.entrySet()) {
            try {
                boolean healthy = entry.getValue().isHealthy();
                healthStatus.put(entry.getKey(), healthy);
                
                if (!healthy) {
                    log.warn("Data source {} is unhealthy", entry.getKey().getDisplayName());
                }
            } catch (Exception e) {
                log.error("Error checking health of data source: {}", entry.getKey(), e);
                healthStatus.put(entry.getKey(), false);
            }
        }
        
        return healthStatus;
    }
    
    /**
     * Gets data sources that support a specific symbol.
     * 
     * @param symbol The symbol to check
     * @return List of data source types that support the symbol
     */
    public List<DataSourceType> getDataSourcesForSymbol(String symbol) {
        return dataSourceRegistry.entrySet().stream()
                .filter(entry -> entry.getValue().supportsSymbol(symbol))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets data sources that support a specific timeframe.
     * 
     * @param timeframe The timeframe to check
     * @return List of data source types that support the timeframe
     */
    public List<DataSourceType> getDataSourcesForTimeframe(String timeframe) {
        return dataSourceRegistry.entrySet().stream()
                .filter(entry -> entry.getValue().supportsTimeframe(timeframe))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
