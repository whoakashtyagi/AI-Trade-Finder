package com.trade.app.datasource.impl;

import com.trade.app.datasource.DataSource;
import com.trade.app.datasource.model.DataSourceConfig;
import com.trade.app.datasource.model.DataSourceResult;
import com.trade.app.datasource.model.DataSourceType;
import com.trade.app.persistence.mongo.document.CoreMarketEvent;
import com.trade.app.persistence.mongo.repository.CoreMarketEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data source implementation for Core Market Events.
 * 
 * Fetches market event data from various technical indicators (RSI, MACD, etc.)
 * stored in the core_market_events collection.
 * 
 * @author AI Trade Finder Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoreMarketEventDataSource implements DataSource {
    
    private final CoreMarketEventRepository coreMarketEventRepository;
    
    // Supported symbols - can be externalized to configuration
    private static final Set<String> SUPPORTED_SYMBOLS = new HashSet<>(Arrays.asList(
        "AAPL", "TSLA", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "SPY", "QQQ", "NQ", "ES", "YM"
    ));
    
    // Supported timeframes
    private static final Set<String> SUPPORTED_TIMEFRAMES = new HashSet<>(Arrays.asList(
        "1m", "5m", "15m", "30m", "1h", "4h", "1d"
    ));
    
    @Override
    public DataSourceResult fetchData(String symbol, String timeframe, DataSourceConfig config) {
        log.debug("Fetching CoreMarketEvent data for symbol: {}, timeframe: {}", symbol, timeframe);
        
        try {
            List<CoreMarketEvent> events = coreMarketEventRepository
                    .findBySymbolAndTimeframeAndIngestedTsBetweenOrderByIngestedTsDesc(
                            symbol,
                            timeframe,
                            config.getFromTime(),
                            config.getToTime()
                    );
            
            // Apply max records limit if specified
            if (config.getMaxRecords() != null && config.getMaxRecords() > 0 && events.size() > config.getMaxRecords()) {
                events = events.subList(0, config.getMaxRecords());
            }
            
            // Apply filter criteria if specified
            if (config.getFilterCriteria() != null && !config.getFilterCriteria().trim().isEmpty()) {
                events = applyFilterCriteria(events, config.getFilterCriteria());
            }
            
            // Transform to generic map format
            List<Map<String, Object>> dataList = events.stream()
                    .map(this::transformToMap)
                    .collect(Collectors.toList());
            
            // Build metadata
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("source", "core_market_events");
            metadata.put("timeRange", config.getFromTime() + " to " + config.getToTime());
            metadata.put("limitApplied", config.getMaxRecords() != null && events.size() == config.getMaxRecords());
            
            return DataSourceResult.builder()
                    .dataSourceType(DataSourceType.CORE_MARKET_EVENT)
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .recordCount(dataList.size())
                    .data(dataList)
                    .metadata(metadata)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching CoreMarketEvent data for {} {}", symbol, timeframe, e);
            
            return DataSourceResult.builder()
                    .dataSourceType(DataSourceType.CORE_MARKET_EVENT)
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .recordCount(0)
                    .data(Collections.emptyList())
                    .success(false)
                    .errorMessage("Failed to fetch data: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.CORE_MARKET_EVENT;
    }
    
    @Override
    public boolean supportsSymbol(String symbol) {
        // If no specific symbols configured, support all
        return SUPPORTED_SYMBOLS.isEmpty() || SUPPORTED_SYMBOLS.contains(symbol.toUpperCase());
    }
    
    @Override
    public boolean supportsTimeframe(String timeframe) {
        return SUPPORTED_TIMEFRAMES.contains(timeframe.toLowerCase());
    }
    
    @Override
    public String getDescription() {
        return "Core market events from technical indicators including RSI, MACD, EMA crossovers, and custom signals";
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - try to count documents
            coreMarketEventRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("CoreMarketEventDataSource health check failed", e);
            return false;
        }
    }
    
    /**
     * Transform CoreMarketEvent to a generic map format.
     */
    private Map<String, Object> transformToMap(CoreMarketEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", event.getIngestedTs().toString());
        map.put("indicator", event.getIndicatorName());
        map.put("message", event.getRawMessage());
        map.put("queued", event.isQueued());
        map.put("transformAttempts", event.getTransformAttempts());
        
        if (event.getMeta() != null && !event.getMeta().isEmpty()) {
            map.put("metadata", event.getMeta());
        }
        
        return map;
    }
    
    /**
     * Apply filter criteria to events.
     * Filter format: "indicator:RSI,MACD" or "queued:false"
     */
    private List<CoreMarketEvent> applyFilterCriteria(List<CoreMarketEvent> events, String filterCriteria) {
        String[] filters = filterCriteria.split(",");
        
        for (String filter : filters) {
            String[] parts = filter.split(":");
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                if ("indicator".equalsIgnoreCase(key)) {
                    Set<String> indicators = Arrays.stream(value.split("\\|"))
                            .map(String::trim)
                            .collect(Collectors.toSet());
                    events = events.stream()
                            .filter(e -> indicators.contains(e.getIndicatorName()))
                            .collect(Collectors.toList());
                } else if ("queued".equalsIgnoreCase(key)) {
                    boolean queuedValue = Boolean.parseBoolean(value);
                    events = events.stream()
                            .filter(e -> e.isQueued() == queuedValue)
                            .collect(Collectors.toList());
                }
            }
        }
        
        return events;
    }
}
