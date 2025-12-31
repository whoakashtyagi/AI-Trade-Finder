package com.trade.app.decision;

import com.trade.app.domain.dto.TradeStatisticsDTO;
import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import com.trade.app.persistence.mongo.repository.IdentifiedTradeRepository;
import com.trade.app.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving and calculating trade statistics.
 * 
 * This service encapsulates the business logic for computing various
 * statistics about identified trades, such as counts, averages, and
 * distributions by symbol and direction.
 * 
 * @author AI Trade Finder Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeStatisticsService {

    private final IdentifiedTradeRepository identifiedTradeRepository;

    /**
     * Retrieves comprehensive trade statistics for the specified time period.
     * 
     * @param hours Number of hours to look back
     * @return TradeStatisticsDTO containing computed statistics
     */
    public TradeStatisticsDTO getStatistics(int hours) {
        log.debug("Computing trade statistics for last {} hours", hours);

        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<IdentifiedTrade> recentTrades = identifiedTradeRepository
                .findByIdentifiedAtAfterOrderByIdentifiedAtDesc(cutoff);

        long totalCount = recentTrades.size();
        long identifiedCount = countByStatus(recentTrades, Constants.TradeStatus.IDENTIFIED);
        long expiredCount = countByStatus(recentTrades, Constants.TradeStatus.EXPIRED);
        long alertedCount = recentTrades.stream()
                .filter(IdentifiedTrade::isAlertSent)
                .count();

        double avgConfidence = calculateAverageConfidence(recentTrades);
        Map<String, Long> bySymbol = groupBySymbol(recentTrades);
        Map<String, Long> byDirection = groupByDirection(recentTrades);

        return TradeStatisticsDTO.builder()
                .periodHours(hours)
                .totalTrades(totalCount)
                .activeTrades(identifiedCount)
                .expiredTrades(expiredCount)
                .alertedTrades(alertedCount)
                .averageConfidence(Math.round(avgConfidence * 10) / 10.0)
                .bySymbol(bySymbol)
                .byDirection(byDirection)
                .build();
    }

    /**
     * Counts trades with a specific status.
     */
    private long countByStatus(List<IdentifiedTrade> trades, String status) {
        return trades.stream()
                .filter(t -> status.equals(t.getStatus()))
                .count();
    }

    /**
     * Calculates average confidence across all trades.
     */
    private double calculateAverageConfidence(List<IdentifiedTrade> trades) {
        return trades.stream()
                .filter(t -> t.getConfidence() != null)
                .mapToInt(IdentifiedTrade::getConfidence)
                .average()
                .orElse(0.0);
    }

    /**
     * Groups trades by symbol and counts occurrences.
     */
    private Map<String, Long> groupBySymbol(List<IdentifiedTrade> trades) {
        Map<String, Long> bySymbol = new HashMap<>();
        trades.forEach(t -> 
            bySymbol.merge(t.getSymbol(), 1L, Long::sum)
        );
        return bySymbol;
    }

    /**
     * Groups trades by direction (LONG/SHORT) and counts occurrences.
     */
    private Map<String, Long> groupByDirection(List<IdentifiedTrade> trades) {
        Map<String, Long> byDirection = new HashMap<>();
        trades.forEach(t -> 
            byDirection.merge(t.getDirection(), 1L, Long::sum)
        );
        return byDirection;
    }
}
