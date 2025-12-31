package com.trade.app.datasource.impl;

import com.trade.app.datasource.DataSource;
import com.trade.app.datasource.model.DataSourceConfig;
import com.trade.app.datasource.model.DataSourceResult;
import com.trade.app.datasource.model.DataSourceType;
import com.trade.app.persistence.mongo.document.OHLCData;
import com.trade.app.persistence.mongo.repository.OHLCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data source implementation for OHLC (Open, High, Low, Close) candlestick data.
 * 
 * Fetches raw price action data at various timeframes for technical analysis.
 * Supports both internal and external data sources.
 * 
 * @author AI Trade Finder Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OHLCDataSource implements DataSource {
    
    private final OHLCRepository ohlcRepository;
    private final MongoTemplate mongoTemplate;
    
    // Timeframe to collection name mapping (for advanced queries)
    private static final Map<String, String> TIMEFRAME_COLLECTIONS = new LinkedHashMap<>();
    
    static {
        TIMEFRAME_COLLECTIONS.put("1m", "ohlc_data_1m");
        TIMEFRAME_COLLECTIONS.put("5m", "ohlc_data_5m");
        TIMEFRAME_COLLECTIONS.put("15m", "ohlc_data_15m");
        TIMEFRAME_COLLECTIONS.put("1h", "ohlc_data_1h");
        TIMEFRAME_COLLECTIONS.put("4h", "ohlc_data_4h");
        TIMEFRAME_COLLECTIONS.put("1d", "ohlc_data_1d");
    }
    
    // Supported symbols
    private static final Set<String> SUPPORTED_SYMBOLS = new HashSet<>(Arrays.asList(
        "NQ", "ES", "YM", "RTY", "CL", "GC", // Futures
        "AAPL", "TSLA", "MSFT", "GOOGL", "AMZN", "META", "NVDA", // Stocks
        "SPY", "QQQ", "IWM", "DIA" // ETFs
    ));
    
    @Override
    public DataSourceResult fetchData(String symbol, String timeframe, DataSourceConfig config) {
        log.debug("Fetching OHLC data for symbol: {}, timeframe: {}, external: {}", 
                symbol, timeframe, config.isUseExternalSource());
        
        try {
            List<OHLCData> ohlcDataList;
            
            // Check if we should use timeframe-specific collections
            String specificCollection = TIMEFRAME_COLLECTIONS.get(timeframe.toLowerCase());
            
            if (specificCollection != null && config.isUseExternalSource()) {
                // Use MongoTemplate for timeframe-specific collection queries
                ohlcDataList = fetchFromSpecificCollection(symbol, timeframe, specificCollection, config);
            } else {
                // Use standard repository query
                ohlcDataList = ohlcRepository.findBySymbolAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                        symbol,
                        timeframe,
                        config.getFromTime(),
                        config.getToTime()
                );
            }
            
            // Apply max records limit if specified
            if (config.getMaxRecords() != null && config.getMaxRecords() > 0 && ohlcDataList.size() > config.getMaxRecords()) {
                ohlcDataList = ohlcDataList.subList(0, config.getMaxRecords());
            }
            
            // Transform to generic map format
            List<Map<String, Object>> dataList = ohlcDataList.stream()
                    .map(this::transformToMap)
                    .collect(Collectors.toList());
            
            // Calculate summary statistics
            Map<String, Object> metadata = buildMetadata(ohlcDataList, config);
            
            return DataSourceResult.builder()
                    .dataSourceType(DataSourceType.OHLC)
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .recordCount(dataList.size())
                    .data(dataList)
                    .metadata(metadata)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching OHLC data for {} {}", symbol, timeframe, e);
            
            return DataSourceResult.builder()
                    .dataSourceType(DataSourceType.OHLC)
                    .symbol(symbol)
                    .timeframe(timeframe)
                    .recordCount(0)
                    .data(Collections.emptyList())
                    .success(false)
                    .errorMessage("Failed to fetch OHLC data: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.OHLC;
    }
    
    @Override
    public boolean supportsSymbol(String symbol) {
        return SUPPORTED_SYMBOLS.isEmpty() || SUPPORTED_SYMBOLS.contains(symbol.toUpperCase());
    }
    
    @Override
    public boolean supportsTimeframe(String timeframe) {
        return TIMEFRAME_COLLECTIONS.containsKey(timeframe.toLowerCase()) || 
               Arrays.asList("1m", "5m", "15m", "30m", "1h", "4h", "1d").contains(timeframe.toLowerCase());
    }
    
    @Override
    public String getDescription() {
        return "OHLC candlestick data with open, high, low, close prices and volume at various timeframes";
    }
    
    @Override
    public boolean isHealthy() {
        try {
            ohlcRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("OHLCDataSource health check failed", e);
            return false;
        }
    }
    
    /**
     * Fetch data from a timeframe-specific collection using MongoTemplate.
     */
    private List<OHLCData> fetchFromSpecificCollection(
            String symbol, 
            String timeframe, 
            String collectionName, 
            DataSourceConfig config) {
        
        long startMillis = config.getFromTime().toEpochMilli();
        long endMillis = config.getToTime().toEpochMilli();
        
        Query query = new Query();
        query.addCriteria(
            Criteria.where("symbol").is(symbol)
                .and("epochMillis").gte(startMillis).lte(endMillis)
        );
        query.with(Sort.by(Sort.Direction.ASC, "epochMillis"));
        
        // Apply limit if specified
        if (config.getMaxRecords() != null && config.getMaxRecords() > 0) {
            query.limit(config.getMaxRecords());
        }
        
        List<org.bson.Document> docs = mongoTemplate.find(query, org.bson.Document.class, collectionName);
        
        // Convert documents to OHLCData objects
        return docs.stream()
                .map(this::documentToOHLCData)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert MongoDB document to OHLCData object.
     */
    private OHLCData documentToOHLCData(org.bson.Document doc) {
        return OHLCData.builder()
                .id(doc.getString("_id"))
                .symbol(doc.getString("symbol"))
                .timeframe(doc.getString("timeframe"))
                .epochMillis(doc.getLong("epochMillis"))
                .timestamp(doc.get("timestamp") != null ? 
                        Instant.parse(doc.get("timestamp").toString()) : null)
                .open(doc.get("open") != null ? 
                        new java.math.BigDecimal(doc.get("open").toString()) : null)
                .high(doc.get("high") != null ? 
                        new java.math.BigDecimal(doc.get("high").toString()) : null)
                .low(doc.get("low") != null ? 
                        new java.math.BigDecimal(doc.get("low").toString()) : null)
                .close(doc.get("close") != null ? 
                        new java.math.BigDecimal(doc.get("close").toString()) : null)
                .volume(doc.getLong("volume"))
                .source(doc.getString("source"))
                .build();
    }
    
    /**
     * Transform OHLCData to a generic map format.
     */
    private Map<String, Object> transformToMap(OHLCData ohlc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("time", ohlc.getTimestamp() != null ? ohlc.getTimestamp().toString() : 
                Instant.ofEpochMilli(ohlc.getEpochMillis()).toString());
        map.put("open", ohlc.getOpen());
        map.put("high", ohlc.getHigh());
        map.put("low", ohlc.getLow());
        map.put("close", ohlc.getClose());
        
        if (ohlc.getVolume() != null) {
            map.put("volume", ohlc.getVolume());
        }
        
        if (ohlc.getSource() != null) {
            map.put("source", ohlc.getSource());
        }
        
        return map;
    }
    
    /**
     * Build metadata with summary statistics.
     */
    private Map<String, Object> buildMetadata(List<OHLCData> ohlcDataList, DataSourceConfig config) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", config.isUseExternalSource() ? "external" : "internal");
        metadata.put("timeRange", config.getFromTime() + " to " + config.getToTime());
        metadata.put("limitApplied", config.getMaxRecords() != null && 
                ohlcDataList.size() == config.getMaxRecords());
        
        if (!ohlcDataList.isEmpty()) {
            OHLCData first = ohlcDataList.get(0);
            OHLCData last = ohlcDataList.get(ohlcDataList.size() - 1);
            
            metadata.put("periodStart", first.getTimestamp().toString());
            metadata.put("periodEnd", last.getTimestamp().toString());
            metadata.put("openPrice", first.getOpen());
            metadata.put("closePrice", last.getClose());
            
            // Calculate high and low for the entire period
            Optional<java.math.BigDecimal> periodHigh = ohlcDataList.stream()
                    .map(OHLCData::getHigh)
                    .max(java.math.BigDecimal::compareTo);
            Optional<java.math.BigDecimal> periodLow = ohlcDataList.stream()
                    .map(OHLCData::getLow)
                    .min(java.math.BigDecimal::compareTo);
            
            periodHigh.ifPresent(high -> metadata.put("periodHigh", high));
            periodLow.ifPresent(low -> metadata.put("periodLow", low));
            
            // Calculate price change percentage
            if (first.getOpen() != null && last.getClose() != null) {
                java.math.BigDecimal priceChange = last.getClose().subtract(first.getOpen());
                java.math.BigDecimal priceChangePercent = priceChange
                        .divide(first.getOpen(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100));
                metadata.put("priceChangePercent", priceChangePercent);
            }
        }
        
        return metadata;
    }
}
