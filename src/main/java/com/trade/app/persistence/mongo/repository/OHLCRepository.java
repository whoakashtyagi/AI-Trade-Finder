package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.OHLCData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB repository for OHLCData documents.
 * 
 * Provides query methods for retrieving OHLC candlestick data based on
 * symbol, timeframe, and time ranges.
 * 
 * @author AI Trade Finder Team
 */
@Repository
public interface OHLCRepository extends MongoRepository<OHLCData, String> {
    
    /**
     * Finds OHLC data for a specific symbol and timeframe within a time range.
     * Results are ordered by timestamp in ascending order.
     * 
     * @param symbol The trading symbol (e.g., "AAPL")
     * @param timeframe The timeframe (e.g., "5m", "1h")
     * @param start Start of the time range (inclusive)
     * @param end End of the time range (inclusive)
     * @return List of OHLC data ordered by timestamp ascending
     */
    List<OHLCData> findBySymbolAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
        String symbol,
        String timeframe,
        Instant start,
        Instant end
    );
    
    /**
     * Finds OHLC data using epoch milliseconds for time range queries.
     * More efficient than Instant-based queries for large datasets.
     * 
     * @param symbol The trading symbol
     * @param timeframe The timeframe
     * @param startMillis Start epoch milliseconds (inclusive)
     * @param endMillis End epoch milliseconds (inclusive)
     * @return List of OHLC data ordered by epoch milliseconds ascending
     */
    List<OHLCData> findBySymbolAndTimeframeAndEpochMillisBetweenOrderByEpochMillisAsc(
        String symbol,
        String timeframe,
        Long startMillis,
        Long endMillis
    );
    
    /**
     * Finds the most recent OHLC data for a symbol and timeframe.
     * 
     * @param symbol The trading symbol
     * @param timeframe The timeframe
     * @return The most recent OHLC data record, or null if none exists
     */
    OHLCData findFirstBySymbolAndTimeframeOrderByTimestampDesc(
        String symbol,
        String timeframe
    );
    
    /**
     * Counts OHLC records for a specific symbol.
     * 
     * @param symbol The trading symbol
     * @return Count of OHLC records
     */
    long countBySymbol(String symbol);
    
    /**
     * Finds all distinct symbols in the OHLC data collection.
     * Note: Use with caution on large datasets.
     * 
     * @return List of unique symbols
     */
    List<String> findDistinctSymbolBy();
}
