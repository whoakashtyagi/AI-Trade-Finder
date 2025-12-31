package com.trade.app.datasource.impl;

import com.trade.app.datasource.DataSource;
import com.trade.app.datasource.model.DataSourceConfig;
import com.trade.app.datasource.model.DataSourceResult;
import com.trade.app.datasource.model.DataSourceType;
import com.trade.app.persistence.mongo.document.TransformedEvent;
import com.trade.app.persistence.mongo.repository.TransformedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data source implementation for Transformed Events.
 * 
 * Fetches transformed market event data that has been enriched with metadata,
 * action codes, feature masks, and trading signal flags. This data source
 * provides processed events ready for AI analysis and trading decisions.
 * 
 * @author AI Trade Finder Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransformedEventDataSource implements DataSource {
    
    private final TransformedEventRepository transformedEventRepository;
    
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
        log.debug("Fetching TransformedEvent data for symbol: {}, timeframe: {}", symbol, timeframe);
        
        try {
            List<TransformedEvent> events;
            
            // Check if we should filter by timeframe or use fallback
            if (timeframe != null && !timeframe.isEmpty()) {
                events = transformedEventRepository
                        .findBySymbolAndTimeframeAndEventTsBetweenOrderByEventTsDesc(
                                symbol,
                                timeframe,
                                config.getFromTime(),
                                config.getToTime()
                        );
            } else {
                // Fallback: find events without timeframe filter
                events = transformedEventRepository
                        .findBySymbolAndEventTsBetweenOrderByEventTsDesc(
                                symbol,
                                config.getFromTime(),
                                config.getToTime()
                        );
            }
            
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
            metadata.put("source", "transformed_events");
            metadata.put("timeRange", config.getFromTime() + " to " + config.getToTime());
            metadata.put("limitApplied", config.getMaxRecords() != null && events.size() == config.getMaxRecords());
            
            return DataSourceResult.builder()
                    .dataSourceType(DataSourceType.TRANSFORMED_EVENT)
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .recordCount(dataList.size())
                    .data(dataList)
                    .metadata(metadata)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching TransformedEvent data for {} {}", symbol, timeframe, e);
            
            return DataSourceResult.builder()
                    .dataSourceType(DataSourceType.TRANSFORMED_EVENT)
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
        return DataSourceType.TRANSFORMED_EVENT;
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
        return "Transformed market events with enriched metadata, action codes, feature masks, and trading signals";
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - try to count documents
            transformedEventRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("TransformedEventDataSource health check failed", e);
            return false;
        }
    }
    
    /**
     * Transform TransformedEvent to a generic map format.
     */
    private Map<String, Object> transformToMap(TransformedEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        
        // Core identifiers
        map.put("timestamp", event.getEventTs().toString());
        map.put("uniqueEventCode", event.getUniqueEventCode());
        map.put("uecDescription", event.getUecDescription());
        
        // Indicator and action information
        map.put("indicatorShortCode", event.getIndicatorShortCode());
        map.put("directionCode", event.getDirectionCode());
        map.put("actionCode", event.getActionCode());
        
        // Feature mask information
        if (event.getFeatureMaskBase36() != null) {
            map.put("featureMaskBase36", event.getFeatureMaskBase36());
        }
        if (event.getFeatureMaskValue() != null) {
            map.put("featureMaskValue", event.getFeatureMaskValue());
        }
        
        // Price information
        if (event.getApproxPriceAtEvent() != null) {
            map.put("approxPrice", event.getApproxPriceAtEvent().toString());
        }
        
        // Flags
        map.put("isTradeSignal", event.isTradeSignal());
        map.put("isTriggerReasoner", event.isTriggerReasoner());
        map.put("isTextNotify", event.isTextNotify());
        map.put("isCallNotify", event.isCallNotify());
        map.put("isAlexaAnnounce", event.isAlexaAnnounce());
        
        // Notification messages (only if present)
        if (event.getAnnounceMsg() != null && !event.getAnnounceMsg().isEmpty()) {
            map.put("announceMsg", event.getAnnounceMsg());
        }
        if (event.getTxtNotifyMsg() != null && !event.getTxtNotifyMsg().isEmpty()) {
            map.put("txtNotifyMsg", event.getTxtNotifyMsg());
        }
        if (event.getCallNotifyMsg() != null && !event.getCallNotifyMsg().isEmpty()) {
            map.put("callNotifyMsg", event.getCallNotifyMsg());
        }
        
        // Extras (if present)
        if (event.getExtras() != null && !event.getExtras().isEmpty()) {
            map.put("extras", event.getExtras());
        }
        
        // Raw event (if needed for debugging)
        if (event.getRawEvent() != null && !event.getRawEvent().isEmpty()) {
            map.put("rawEvent", event.getRawEvent());
        }
        
        return map;
    }
    
    /**
     * Apply filter criteria to events.
     * 
     * Supported filter formats:
     * - "actionCode:BUY|SELL" - Filter by action codes
     * - "directionCode:UP|DOWN" - Filter by direction codes
     * - "isTradeSignal:true" - Filter by trade signal flag
     * - "indicatorShortCode:RSI|MACD" - Filter by indicator codes
     * - "uniqueEventCode:ABC123" - Filter by unique event code
     * 
     * Multiple filters can be combined with commas.
     */
    private List<TransformedEvent> applyFilterCriteria(List<TransformedEvent> events, String filterCriteria) {
        String[] filters = filterCriteria.split(",");
        
        for (String filter : filters) {
            String[] parts = filter.split(":");
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                
                switch (key.toLowerCase()) {
                    case "actioncode":
                    case "action_code":
                        Set<String> actionCodes = Arrays.stream(value.split("\\|"))
                                .map(String::trim)
                                .map(String::toUpperCase)
                                .collect(Collectors.toSet());
                        events = events.stream()
                                .filter(e -> e.getActionCode() != null && actionCodes.contains(e.getActionCode().toUpperCase()))
                                .collect(Collectors.toList());
                        break;
                        
                    case "directioncode":
                    case "direction_code":
                        Set<String> directionCodes = Arrays.stream(value.split("\\|"))
                                .map(String::trim)
                                .map(String::toUpperCase)
                                .collect(Collectors.toSet());
                        events = events.stream()
                                .filter(e -> e.getDirectionCode() != null && directionCodes.contains(e.getDirectionCode().toUpperCase()))
                                .collect(Collectors.toList());
                        break;
                        
                    case "istradesignal":
                    case "is_trade_signal":
                    case "tradesignal":
                        boolean isTradeSignal = Boolean.parseBoolean(value);
                        events = events.stream()
                                .filter(e -> e.isTradeSignal() == isTradeSignal)
                                .collect(Collectors.toList());
                        break;
                        
                    case "indicatorshortcode":
                    case "indicator_short_code":
                    case "indicator":
                        Set<String> indicators = Arrays.stream(value.split("\\|"))
                                .map(String::trim)
                                .map(String::toUpperCase)
                                .collect(Collectors.toSet());
                        events = events.stream()
                                .filter(e -> e.getIndicatorShortCode() != null && indicators.contains(e.getIndicatorShortCode().toUpperCase()))
                                .collect(Collectors.toList());
                        break;
                        
                    case "uniqueeventcode":
                    case "unique_event_code":
                    case "uec":
                        events = events.stream()
                                .filter(e -> e.getUniqueEventCode() != null && e.getUniqueEventCode().equalsIgnoreCase(value))
                                .collect(Collectors.toList());
                        break;
                        
                    default:
                        log.warn("Unknown filter key: {}", key);
                        break;
                }
            }
        }
        
        return events;
    }
}
