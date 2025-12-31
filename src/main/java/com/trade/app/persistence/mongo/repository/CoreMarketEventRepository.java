package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.CoreMarketEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB repository for CoreMarketEvent documents.
 * 
 * Provides specialized query methods for retrieving market events based on
 * symbol, timeframe, and time ranges. This repository supports efficient
 * queries for time-series market data analysis.
 * 
 * @author AI Trade Finder Team
 */
@Repository
public interface CoreMarketEventRepository extends MongoRepository<CoreMarketEvent, String> {
    
    /**
     * Retrieves all core market events from the database.
     * 
     * @return List of all CoreMarketEvent documents
     */
    @Override
    List<CoreMarketEvent> findAll();

    /**
     * Finds market events for a specific symbol, timeframe, and time range.
     * Results are ordered by ingestion timestamp in descending order (newest first).
     * 
     * This method is useful for retrieving recent events for a specific symbol
     * and timeframe within a given time window.
     * 
     * @param symbol The trading symbol to filter by (e.g., "AAPL")
     * @param timeframe The timeframe to filter by (e.g., "5m", "1h")
     * @param start The start of the time range (inclusive)
     * @param end The end of the time range (inclusive)
     * @return List of CoreMarketEvent documents matching the criteria, ordered by ingestedTs DESC
     */
    List<CoreMarketEvent> findBySymbolAndTimeframeAndIngestedTsBetweenOrderByIngestedTsDesc(
        String symbol,
        String timeframe,
        Instant start,
        Instant end
    );

    /**
     * Finds market events for a specific symbol within a time range, regardless of timeframe.
     * Results are ordered by ingestion timestamp in descending order (newest first).
     * 
     * This method is useful for retrieving all events for a symbol across different
     * timeframes within a given time window.
     * 
     * @param symbol The trading symbol to filter by (e.g., "TSLA")
     * @param start The start of the time range (inclusive)
     * @param end The end of the time range (inclusive)
     * @return List of CoreMarketEvent documents matching the criteria, ordered by ingestedTs DESC
     */
    List<CoreMarketEvent> findBySymbolAndIngestedTsBetweenOrderByIngestedTsDesc(
        String symbol, 
        Instant start, 
        Instant end
    );
}
