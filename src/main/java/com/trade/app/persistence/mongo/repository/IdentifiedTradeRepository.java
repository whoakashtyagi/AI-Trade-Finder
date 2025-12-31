package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for IdentifiedTrade documents.
 * 
 * Provides query methods for retrieving and managing identified trading opportunities,
 * including deduplication checks and status-based filtering.
 * 
 * @author AI Trade Finder Team
 */
@Repository
public interface IdentifiedTradeRepository extends MongoRepository<IdentifiedTrade, String> {

    /**
     * Finds an identified trade by its deduplication key.
     * Used to prevent duplicate alerts for the same trade setup.
     * 
     * @param dedupeKey The unique deduplication key
     * @return Optional containing the IdentifiedTrade if found
     */
    Optional<IdentifiedTrade> findByDedupeKey(String dedupeKey);

    /**
     * Finds all identified trades for a symbol with a specific status.
     * Results are ordered by identification time in descending order (newest first).
     * 
     * @param symbol The trading symbol
     * @param status The trade status (e.g., "IDENTIFIED", "ALERTED", "EXPIRED")
     * @return List of identified trades matching the criteria
     */
    List<IdentifiedTrade> findBySymbolAndStatusOrderByIdentifiedAtDesc(
        String symbol, 
        String status
    );

    /**
     * Finds all identified trades after a specific time.
     * Useful for retrieving recent trade identifications.
     * 
     * @param cutoff The cutoff timestamp
     * @return List of identified trades after the cutoff time, ordered by identifiedAt DESC
     */
    List<IdentifiedTrade> findByIdentifiedAtAfterOrderByIdentifiedAtDesc(Instant cutoff);

    Page<IdentifiedTrade> findByIdentifiedAtAfterOrderByIdentifiedAtDesc(Instant cutoff, Pageable pageable);

    /**
     * Finds expired trades that are still marked as IDENTIFIED.
     * These trades should have their status updated to EXPIRED.
     * 
     * @param now The current timestamp
     * @return List of trades that have expired
     */
    @Query("{'status': 'IDENTIFIED', 'expiresAt': {$lt: ?0}}")
    List<IdentifiedTrade> findExpiredTrades(Instant now);

    /**
     * Finds all identified trades for a specific symbol.
     * 
     * @param symbol The trading symbol
     * @return List of all identified trades for the symbol
     */
    List<IdentifiedTrade> findBySymbolOrderByIdentifiedAtDesc(String symbol);

    Page<IdentifiedTrade> findBySymbolOrderByIdentifiedAtDesc(String symbol, Pageable pageable);

    Page<IdentifiedTrade> findBySymbolAndStatusOrderByIdentifiedAtDesc(String symbol, String status, Pageable pageable);

    Page<IdentifiedTrade> findByStatusOrderByIdentifiedAtDesc(String status, Pageable pageable);

    @Query("{'confidence': {$gte: ?0}}")
    Page<IdentifiedTrade> findByConfidenceGreaterThanEqual(Integer minConfidence, Pageable pageable);

    /**
     * Finds identified trades by symbol and direction.
     * 
     * @param symbol The trading symbol
     * @param direction The trade direction (LONG or SHORT)
     * @return List of identified trades matching the criteria
     */
    List<IdentifiedTrade> findBySymbolAndDirectionOrderByIdentifiedAtDesc(
        String symbol, 
        String direction
    );

    /**
     * Finds identified trades by status within a time range.
     * 
     * @param status The trade status
     * @param start Start of time range
     * @param end End of time range
     * @return List of identified trades matching the criteria
     */
    List<IdentifiedTrade> findByStatusAndIdentifiedAtBetweenOrderByIdentifiedAtDesc(
        String status, 
        Instant start, 
        Instant end
    );

    /**
     * Finds identified trades with confidence above a threshold.
     * 
     * @param minConfidence Minimum confidence level
     * @param status Optional status filter
     * @return List of high-confidence trades
     */
    @Query("{'confidence': {$gte: ?0}, 'status': ?1}")
    List<IdentifiedTrade> findByConfidenceGreaterThanEqualAndStatus(
        Integer minConfidence, 
        String status
    );

    /**
     * Counts identified trades for a symbol and status.
     * 
     * @param symbol The trading symbol
     * @param status The trade status
     * @return Count of matching trades
     */
    long countBySymbolAndStatus(String symbol, String status);

    /**
     * Checks if a dedupe key already exists.
     * 
     * @param dedupeKey The deduplication key
     * @return true if exists, false otherwise
     */
    boolean existsByDedupeKey(String dedupeKey);
}
