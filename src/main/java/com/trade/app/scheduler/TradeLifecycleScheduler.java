package com.trade.app.scheduler;

import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import com.trade.app.persistence.mongo.repository.IdentifiedTradeRepository;
import com.trade.app.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler service for managing trade lifecycle.
 * 
 * This service handles periodic tasks related to identified trades:
 * - Expiring old trade setups
 * - Cleanup of stale data
 * - Status updates
 * 
 * @author AI Trade Finder Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeLifecycleScheduler {

    private final IdentifiedTradeRepository identifiedTradeRepository;

    /**
     * Expires trades that have passed their expiration time.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void expireTrades() {
        log.debug("Running trade expiration check");

        try {
            Instant now = Instant.now();
            List<IdentifiedTrade> expiredTrades = identifiedTradeRepository.findExpiredTrades(now);

            if (expiredTrades.isEmpty()) {
                log.debug("No expired trades found");
                return;
            }

            log.info("Found {} expired trades, updating status", expiredTrades.size());

            for (IdentifiedTrade trade : expiredTrades) {
                trade.setStatus("EXPIRED");
                trade.setUpdatedAt(now);
                identifiedTradeRepository.save(trade);

                log.info("Expired trade: {} {} at {} (confidence: {})",
                    trade.getSymbol(), trade.getDirection(), trade.getEntryZone(), trade.getConfidence());
            }

        } catch (Exception e) {
            log.error("Error expiring trades: {}", e.getMessage(), e);
        }
    }

    /**
     * Provides summary statistics about identified trades.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void logTradeStatistics() {
        try {
            Instant last24Hours = Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
            List<IdentifiedTrade> recentTrades = identifiedTradeRepository
                .findByIdentifiedAtAfterOrderByIdentifiedAtDesc(last24Hours);

            long identified = recentTrades.stream()
                .filter(t -> Constants.TradeStatus.IDENTIFIED.equals(t.getStatus()))
                .count();

            long expired = recentTrades.stream()
                .filter(t -> Constants.TradeStatus.EXPIRED.equals(t.getStatus()))
                .count();

            long alerted = recentTrades.stream()
                .filter(IdentifiedTrade::isAlertSent)
                .count();

            double avgConfidence = recentTrades.stream()
                .mapToInt(t -> t.getConfidence() != null ? t.getConfidence() : 0)
                .average()
                .orElse(0.0);

            log.info("=== Trade Statistics (Last 24h) ===");
            log.info("Total trades: {}", recentTrades.size());
            log.info("Active (IDENTIFIED): {}", identified);
            log.info("Expired: {}", expired);
            log.info("Alerted: {}", alerted);
            log.info("Avg confidence: {:.1f}%", avgConfidence);
            log.info("===================================");

        } catch (Exception e) {
            log.error("Error generating trade statistics: {}", e.getMessage(), e);
        }
    }
}
