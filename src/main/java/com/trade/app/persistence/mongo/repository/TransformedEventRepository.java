package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.TransformedEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository for TransformedEvent documents.
 * 
 * Provides specialized query methods for retrieving transformed market events
 * based on symbol, timeframe, and time ranges. These events represent processed
 * and enriched market data ready for trading signal generation and analysis.
 * 
 * @author AI Trade Finder Team
 */
@Repository
public interface TransformedEventRepository extends MongoRepository<TransformedEvent, String> {
    
    /**
     * Retrieves all transformed events from the database.
     * 
     * @return List of all TransformedEvent documents
     */
    @Override
    List<TransformedEvent> findAll();

    /**
     * Finds transformed events for a specific symbol, timeframe, and time range.
     * Results are ordered by event timestamp in descending order (newest first).
     * 
     * This is the primary query method for retrieving transformed events for
     * a specific symbol and timeframe within a time window.
     * 
     * @param symbol The trading symbol to filter by (e.g., "AAPL", "NQ")
     * @param timeframe The timeframe to filter by (e.g., "5m", "1h", "1d")
     * @param start The start of the time range (inclusive)
     * @param end The end of the time range (inclusive)
     * @return List of TransformedEvent documents matching the criteria, ordered by eventTs DESC
     */
    List<TransformedEvent> findBySymbolAndTimeframeAndEventTsBetweenOrderByEventTsDesc(
        String symbol,
        String timeframe,
        Instant start,
        Instant end
    );

    /**
     * Fallback method: Finds events for a symbol within a time range,
     * ignoring the timeframe filter.
     * 
     * Useful when you want all events for a symbol across all timeframes,
     * or when timeframe data is not available.
     * 
     * @param symbol The trading symbol to filter by
     * @param start The start of the time range (inclusive)
     * @param end The end of the time range (inclusive)
     * @return List of TransformedEvent documents matching the criteria, ordered by eventTs DESC
     */
    List<TransformedEvent> findBySymbolAndEventTsBetweenOrderByEventTsDesc(
        String symbol,
        Instant start,
        Instant end
    );

    /**
     * Finds transformed events by unique event code for pattern analysis.
     * 
     * @param uniqueEventCode The unique event code to filter by
     * @param start The start of the time range (inclusive)
     * @param end The end of the time range (inclusive)
     * @return List of TransformedEvent documents matching the criteria
     */
    List<TransformedEvent> findByUniqueEventCodeAndEventTsBetweenOrderByEventTsDesc(
        String uniqueEventCode,
        Instant start,
        Instant end
    );

    /**
     * Finds transformed events that are trade signals.
     * Useful for filtering only actionable trading signals.
     * 
     * @param symbol The trading symbol to filter by
     * @param timeframe The timeframe to filter by
     * @param isTradeSignal Filter by trade signal flag (typically true)
     * @param start The start of the time range (inclusive)
     * @param end The end of the time range (inclusive)
     * @return List of TransformedEvent documents that are trade signals
     */
    List<TransformedEvent> findBySymbolAndTimeframeAndIsTradeSignalAndEventTsBetweenOrderByEventTsDesc(
        String symbol,
        String timeframe,
        boolean isTradeSignal,
        Instant start,
        Instant end
    );

    /**
     * Finds transformed events for a symbol after a specific time.
     * This is useful for the AI Trade Finder to retrieve recent events
     * across all timeframes for a symbol.
     * 
     * @param symbol The trading symbol to filter by
     * @param cutoffTime The cutoff time (events after this time are returned)
     * @return List of TransformedEvent documents after the cutoff, ordered by eventTs DESC
     */
    List<TransformedEvent> findBySymbolAndEventTsAfterOrderByEventTsDesc(
        String symbol,
        Instant cutoffTime
    );

    /**
     * Finds transformed events for a symbol and specific timeframe after a cutoff time.
     * 
     * @param symbol The trading symbol to filter by
     * @param timeframe The timeframe to filter by
     * @param cutoffTime The cutoff time
     * @return List of TransformedEvent documents matching criteria, ordered by eventTs DESC
     */
    List<TransformedEvent> findBySymbolAndTimeframeAndEventTsAfterOrderByEventTsDesc(
        String symbol,
        String timeframe,
        Instant cutoffTime
    );
}
