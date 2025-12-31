package com.trade.app.persistence.mongo.document;

import java.time.Instant;
import java.util.Map;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB document representing core market events from various indicators.
 * 
 * This document stores raw market events that have been ingested from external sources
 * and are pending processing or transformation. Each event is associated with a symbol,
 * timeframe, and indicator name for efficient querying.
 * 
 * @author AI Trade Finder Team
 */
@Document("core_market_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreMarketEvent {

    /**
     * Unique identifier for the market event.
     */
    @Id
    private String id;

    /**
     * Trading symbol (e.g., "AAPL", "TSLA").
     * Indexed for efficient symbol-based queries.
     */
    @Indexed
    @Field("symbol")
    private String symbol;

    /**
     * Timeframe for the market data (e.g., "1m", "5m", "1h", "1d").
     * Indexed for efficient timeframe-based queries.
     */
    @Indexed
    @Field("timeframe")
    private String timeframe;

    /**
     * Name of the indicator that generated this event (e.g., "RSI", "MACD", "EMA").
     * Indexed for efficient indicator-based queries.
     */
    @Indexed
    @Field("indicator_name")
    private String indicatorName;

    /**
     * Raw message/payload from the source system.
     * Contains the unprocessed event data.
     */
    @Field("raw_message")
    private String rawMessage;

    /**
     * Timestamp when this event was ingested into the system.
     * Indexed for efficient time-range queries.
     * Defaults to current time when the event is created.
     */
    @Indexed
    @Field("ingested_ts")
    @Builder.Default
    private Instant ingestedTs = Instant.now();

    /**
     * Additional metadata associated with the event.
     * Flexible storage for any extra information needed for processing.
     */
    @Field("meta")
    private Map<String, Object> meta;

    /**
     * Flag indicating whether this event has been queued for processing.
     * Defaults to false for newly ingested events.
     */
    @Field("queued")
    @Builder.Default
    private boolean queued = false;

    /**
     * Number of times transformation/processing has been attempted for this event.
     * Used for retry logic and error tracking.
     * Defaults to 0 for new events.
     */
    @Field("transform_attempts")
    @Builder.Default
    private int transformAttempts = 0;
}
