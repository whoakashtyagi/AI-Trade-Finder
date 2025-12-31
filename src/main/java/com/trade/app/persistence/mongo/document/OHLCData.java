package com.trade.app.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * MongoDB document representing OHLC (Open, High, Low, Close) candlestick data.
 * 
 * This document stores raw price action data at various timeframes for technical analysis.
 * Each record represents a single candlestick with OHLC prices and optional volume data.
 * 
 * @author AI Trade Finder Team
 */
@Document("ohlc_data")
@CompoundIndex(name = "symbol_timestamp_idx", def = "{'symbol': 1, 'timestamp': 1}")
@CompoundIndex(name = "symbol_timeframe_epoch_idx", def = "{'symbol': 1, 'timeframe': 1, 'epochMillis': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OHLCData {
    
    /**
     * Unique identifier: symbol + epochMillis (e.g., "AAPL_1672531199000").
     */
    @Id
    private String id;
    
    /**
     * Trading symbol (e.g., "AAPL", "NQ", "ES").
     */
    @Indexed
    @Field("symbol")
    private String symbol;
    
    /**
     * Timeframe for this candlestick (e.g., "1m", "5m", "1h", "1d").
     */
    @Indexed
    @Field("timeframe")
    private String timeframe;
    
    /**
     * Timestamp in milliseconds since epoch.
     * Used for efficient range queries.
     */
    @Indexed
    @Field("epoch_millis")
    private Long epochMillis;
    
    /**
     * Timestamp as Instant (ISO-8601 format).
     * Represents the opening time of the candlestick.
     */
    @Indexed
    @Field("timestamp")
    private Instant timestamp;
    
    /**
     * Opening price of the candlestick.
     */
    @Field("open")
    private BigDecimal open;
    
    /**
     * Highest price during the candlestick period.
     */
    @Field("high")
    private BigDecimal high;
    
    /**
     * Lowest price during the candlestick period.
     */
    @Field("low")
    private BigDecimal low;
    
    /**
     * Closing price of the candlestick.
     */
    @Field("close")
    private BigDecimal close;
    
    /**
     * Trading volume during the candlestick period (optional).
     */
    @Field("volume")
    private Long volume;
    
    /**
     * Data source identifier (e.g., "DATABENTO", "INTERNAL", "POLYGON").
     */
    @Field("source")
    private String source;
    
    /**
     * Record type identifier (optional, for external data providers).
     */
    @Field("rtype")
    private Integer rtype;
    
    /**
     * Publisher ID (optional, for external data providers).
     */
    @Field("publisher_id")
    private Integer publisherId;
    
    /**
     * Instrument ID (optional, for external data providers).
     */
    @Field("instrument_id")
    private Long instrumentId;
}
