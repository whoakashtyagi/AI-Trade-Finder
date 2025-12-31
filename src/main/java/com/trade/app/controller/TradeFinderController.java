package com.trade.app.controller;

import com.trade.app.decision.AITradeFinderService;
import com.trade.app.decision.TradeStatisticsService;
import com.trade.app.domain.dto.ApiResponse;
import com.trade.app.domain.dto.TradeStatisticsDTO;
import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import com.trade.app.persistence.mongo.repository.IdentifiedTradeRepository;
import com.trade.app.service.OperationLogService;
import com.trade.app.util.OperationLogContext;
import com.trade.app.util.Constants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
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
     * UI-friendly list endpoint with optional filters.
     *
     * GET /api/v1/trade-finder/trades?symbol=&status=&direction=&minConfidence=&hours=24&limit=200
     */
    @GetMapping("/trades")
    public ResponseEntity<List<IdentifiedTrade>> listTrades(
        @RequestParam(required = false) String symbol,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String direction,
        @RequestParam(required = false) Integer minConfidence,
        @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours,
        @RequestParam(defaultValue = "200") @Min(1) @Max(1000) int limit
    ) {
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "identifiedAt"));

        if (symbol != null && !symbol.isBlank() && status != null && !status.isBlank()) {
            return ResponseEntity.ok(
                identifiedTradeRepository.findBySymbolAndStatusOrderByIdentifiedAtDesc(symbol, status, page).getContent()
            );
        }

        List<IdentifiedTrade> base;
        if (symbol != null && !symbol.isBlank()) {
            base = identifiedTradeRepository.findBySymbolOrderByIdentifiedAtDesc(symbol, page).getContent();
        } else if (status != null && !status.isBlank()) {
            base = identifiedTradeRepository.findByStatusOrderByIdentifiedAtDesc(status, page).getContent();
        } else if (minConfidence != null) {
            base = identifiedTradeRepository.findByConfidenceGreaterThanEqual(minConfidence, page).getContent();
        } else {
            base = identifiedTradeRepository.findByIdentifiedAtAfterOrderByIdentifiedAtDesc(cutoff, page).getContent();
        }

        List<IdentifiedTrade> filtered = base.stream()
            .filter(t -> t.getIdentifiedAt() == null || !t.getIdentifiedAt().isBefore(cutoff))
            .filter(t -> direction == null || direction.isBlank() || direction.equalsIgnoreCase(t.getDirection()))
            .filter(t -> minConfidence == null || (t.getConfidence() != null && t.getConfidence() >= minConfidence))
            .toList();

        return ResponseEntity.ok(filtered);
    }

    /**
     * Get a trade by Mongo id without path ambiguity with /trades/{symbol}.
     *
     * GET /api/v1/trade-finder/trades/id/{tradeId}
     */
    @GetMapping("/trades/id/{tradeId}")
    public ResponseEntity<IdentifiedTrade> getTradeById(@PathVariable String tradeId) {
        return identifiedTradeRepository.findById(tradeId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Update trade status from the UI.
     *
     * PATCH /api/v1/trade-finder/trades/id/{tradeId}/status
     */
    @PatchMapping("/trades/id/{tradeId}/status")
    public ResponseEntity<IdentifiedTrade> updateTradeStatus(
        @PathVariable String tradeId,
        @RequestBody UpdateTradeStatusRequest request
    ) {
        String operationId = operationLogService.startOrReuse(
            "TRADE_STATUS_UPDATE",
            "Update trade status: " + tradeId,
            "API",
            Map.of(
                "endpoint", "/api/v1/trade-finder/trades/id/{tradeId}/status",
                "tradeId", tradeId,
                "newStatus", request != null ? request.getStatus() : null
            )
        );

        try {
            IdentifiedTrade trade = identifiedTradeRepository.findById(tradeId).orElse(null);
            if (trade == null) {
                operationLogService.completeFailure(operationId, "Trade not found", new RuntimeException("Trade not found"), null);
                return ResponseEntity.notFound().build();
            }

            if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
                operationLogService.completeFailure(operationId, "Missing status", new IllegalArgumentException("status is required"), null);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            trade.setStatus(request.getStatus());
            trade.setUpdatedAt(Instant.now());

            if (request.getAlertSent() != null) {
                trade.setAlertSent(request.getAlertSent());
                trade.setAlertSentAt(request.getAlertSent() ? Instant.now() : null);
            }

            if (request.getAlertType() != null) {
                trade.setAlertType(request.getAlertType());
            }

            IdentifiedTrade saved = identifiedTradeRepository.save(trade);
            operationLogService.completeSuccess(operationId, "Trade status updated", Map.of(
                "tradeId", saved.getId(),
                "status", saved.getStatus()
            ));

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            operationLogService.completeFailure(operationId, "Trade status update failed", e, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            OperationLogContext.clear();
        }
    }

    /**
     * Trade summary for UI dashboards.
     *
     * GET /api/v1/trade-finder/summary?hours=24
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> tradeSummary(
        @RequestParam(defaultValue = "24") @Min(1) @Max(168) int hours
    ) {
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<IdentifiedTrade> recent = identifiedTradeRepository.findByIdentifiedAtAfterOrderByIdentifiedAtDesc(cutoff);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (IdentifiedTrade t : recent) {
            String s = t.getStatus() != null ? t.getStatus() : "UNKNOWN";
            byStatus.put(s, byStatus.getOrDefault(s, 0L) + 1);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hours", hours);
        response.put("total", recent.size());
        response.put("byStatus", byStatus);
        response.put("knownStatuses", List.of(
            Constants.TradeStatus.IDENTIFIED,
            Constants.TradeStatus.ALERTED,
            Constants.TradeStatus.EXPIRED,
            Constants.TradeStatus.CANCELLED,
            Constants.TradeStatus.TAKEN,
            Constants.TradeStatus.INVALIDATED
        ));

        return ResponseEntity.ok(response);
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

    @lombok.Data
    public static class UpdateTradeStatusRequest {
        private String status;
        private Boolean alertSent;
        private String alertType;
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
