package com.trade.app.controller;

import com.trade.app.decision.AITradeFinderService;
import com.trade.app.decision.TradeStatisticsService;
import com.trade.app.domain.dto.ApiResponse;
import com.trade.app.domain.dto.TradeStatisticsDTO;
import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import com.trade.app.persistence.mongo.repository.IdentifiedTradeRepository;
import com.trade.app.service.OperationLogService;
import com.trade.app.util.OperationLogContext;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for AI Trade Finder operations.
 * 
 * Provides endpoints for:
 * - Manual trigger of trade finding
 * - Retrieving identified trades
 * - Trade statistics
 * 
 * @author AI Trade Finder Team
 */
@RestController
@RequestMapping("/api/v1/trade-finder")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TradeFinderController {

    private final AITradeFinderService tradeFinderService;
    private final IdentifiedTradeRepository identifiedTradeRepository;
    private final TradeStatisticsService tradeStatisticsService;
    private final OperationLogService operationLogService;

    /**
     * Manually triggers the trade finder for all symbols.
     * 
     * @return Response with execution status
     */
    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerTradeFinder() {
        log.info("Manual trigger of AI Trade Finder requested");

        String operationId = operationLogService.startOrReuse(
            "TRADE_FINDER_TRIGGER",
            "Manual trade finder trigger",
            "MANUAL",
            Map.of("endpoint", "/api/v1/trade-finder/trigger")
        );

        long startTime = System.currentTimeMillis();
        try {
            tradeFinderService.findTrades();
            operationLogService.completeSuccess(operationId, "Trade finder trigger completed", Map.of(
                "durationMs", System.currentTimeMillis() - startTime
            ));
        } catch (Exception e) {
            operationLogService.completeFailure(operationId, "Trade finder trigger failed", e, Map.of(
                "durationMs", System.currentTimeMillis() - startTime
            ));
            throw e;
        } finally {
            OperationLogContext.clear();
        }
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> executionDetails = new HashMap<>();
        executionDetails.put("duration_ms", duration);
        executionDetails.put("operationId", operationId);

        return ResponseEntity.ok(
            ApiResponse.success("Trade finder execution completed", executionDetails)
        );
    }

    /**
     * Retrieves all identified trades for a symbol.
     * 
     * @param symbol The trading symbol
     * @return List of identified trades
     */
    @GetMapping("/trades/{symbol}")
    public ResponseEntity<List<IdentifiedTrade>> getTradesBySymbol(@PathVariable String symbol) {
        log.debug("Fetching trades for symbol: {}", symbol);

        List<IdentifiedTrade> trades = identifiedTradeRepository
            .findBySymbolOrderByIdentifiedAtDesc(symbol);

        return ResponseEntity.ok(trades);
    }

    /**
     * Retrieves all identified trades with a specific status.
     * 
     * @param symbol The trading symbol
     * @param status The trade status
     * @return List of identified trades
     */
    @GetMapping("/trades/{symbol}/status/{status}")
    public ResponseEntity<List<IdentifiedTrade>> getTradesBySymbolAndStatus(
        @PathVariable String symbol,
        @PathVariable String status
    ) {
        log.debug("Fetching trades for symbol: {}, status: {}", symbol, status);

        List<IdentifiedTrade> trades = identifiedTradeRepository
            .findBySymbolAndStatusOrderByIdentifiedAtDesc(symbol, status);

        return ResponseEntity.ok(trades);
    }

    /**
     * Retrieves recent identified trades across all symbols.
     * 
     * @param hours Number of hours to look back (default: 24, min: 1, max: 168)
     * @return List of recent trades
     */
    @GetMapping("/trades/recent")
    public ResponseEntity<List<IdentifiedTrade>> getRecentTrades(
        @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours
    ) {
        log.debug("Fetching trades from last {} hours", hours);

        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<IdentifiedTrade> trades = identifiedTradeRepository
            .findByIdentifiedAtAfterOrderByIdentifiedAtDesc(cutoff);

        return ResponseEntity.ok(trades);
    }

    /**
     * Retrieves statistics about identified trades.
     * 
     * @param hours Number of hours to look back (default: 24, min: 1, max: 168)
     * @return Statistics DTO
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<TradeStatisticsDTO>> getStatistics(
            @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours) {
        log.debug("Fetching trade statistics for last {} hours", hours);

        TradeStatisticsDTO statistics = tradeStatisticsService.getStatistics(hours);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * Health check endpoint for the trade finder service.
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> health() {
        return ResponseEntity.ok(
            ApiResponse.success("AI Trade Finder service is running")
        );
    }
}
