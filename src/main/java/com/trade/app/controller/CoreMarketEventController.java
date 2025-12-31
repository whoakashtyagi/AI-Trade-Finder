package com.trade.app.controller;

import com.trade.app.persistence.mongo.document.CoreMarketEvent;
import com.trade.app.persistence.mongo.repository.CoreMarketEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Core Market Event operations.
 * 
 * This controller provides endpoints for managing and querying core market events
 * that have been ingested from external data sources. It supports CRUD operations
 * as well as specialized queries for time-series market data analysis.
 * 
 * @author AI Trade Finder Team
 */
@RestController
@RequestMapping("/api/v1/market-events")
@RequiredArgsConstructor
@Slf4j
public class CoreMarketEventController {
    
    private final CoreMarketEventRepository coreMarketEventRepository;
    
    /**
     * Creates a new core market event.
     * 
     * POST /api/v1/market-events
     * 
     * Example request body:
     * {
     *   "symbol": "AAPL",
     *   "timeframe": "5m",
     *   "indicatorName": "RSI",
     *   "rawMessage": "{\"value\": 65.3, \"signal\": \"neutral\"}",
     *   "meta": {"source": "tradingview"}
     * }
     * 
     * @param event The CoreMarketEvent to create
     * @return ResponseEntity with the created event
     */
    @PostMapping
    public ResponseEntity<CoreMarketEvent> createEvent(@RequestBody CoreMarketEvent event) {
        try {
            log.info("Creating new market event for symbol: {}, timeframe: {}, indicator: {}", 
                    event.getSymbol(), event.getTimeframe(), event.getIndicatorName());
            
            CoreMarketEvent savedEvent = coreMarketEventRepository.save(event);
            
            log.info("Successfully created market event with ID: {}", savedEvent.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
            
        } catch (Exception e) {
            log.error("Error creating market event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retrieves all core market events.
     * 
     * GET /api/v1/market-events
     * 
     * @return ResponseEntity with list of all events
     */
    @GetMapping
    public ResponseEntity<List<CoreMarketEvent>> getAllEvents() {
        try {
            log.debug("Retrieving all market events");
            List<CoreMarketEvent> events = coreMarketEventRepository.findAll();
            log.debug("Found {} market events", events.size());
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            log.error("Error retrieving market events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retrieves a specific market event by ID.
     * 
     * GET /api/v1/market-events/{id}
     * 
     * @param id The event ID
     * @return ResponseEntity with the event or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<CoreMarketEvent> getEventById(@PathVariable String id) {
        try {
            log.debug("Retrieving market event with ID: {}", id);
            Optional<CoreMarketEvent> event = coreMarketEventRepository.findById(id);
            
            return event.map(ResponseEntity::ok)
                       .orElseGet(() -> {
                           log.warn("Market event not found with ID: {}", id);
                           return ResponseEntity.notFound().build();
                       });
                       
        } catch (Exception e) {
            log.error("Error retrieving market event by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retrieves market events for a specific symbol within a time range.
     * 
     * GET /api/v1/market-events/symbol/{symbol}?start={start}&end={end}
     * 
     * Example: GET /api/v1/market-events/symbol/AAPL?start=2025-01-01T00:00:00Z&end=2025-01-31T23:59:59Z
     * 
     * @param symbol The trading symbol
     * @param start Start of time range (ISO-8601 format)
     * @param end End of time range (ISO-8601 format)
     * @return ResponseEntity with list of matching events
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<CoreMarketEvent>> getEventsBySymbol(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        try {
            log.info("Retrieving market events for symbol: {} between {} and {}", symbol, start, end);
            
            List<CoreMarketEvent> events = coreMarketEventRepository
                    .findBySymbolAndIngestedTsBetweenOrderByIngestedTsDesc(symbol, start, end);
            
            log.info("Found {} market events for symbol: {}", events.size(), symbol);
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            log.error("Error retrieving market events for symbol: {}", symbol, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retrieves market events for a specific symbol, timeframe, and time range.
     * 
     * GET /api/v1/market-events/symbol/{symbol}/timeframe/{timeframe}?start={start}&end={end}
     * 
     * Example: GET /api/v1/market-events/symbol/TSLA/timeframe/5m?start=2025-01-01T00:00:00Z&end=2025-01-01T23:59:59Z
     * 
     * @param symbol The trading symbol
     * @param timeframe The timeframe (e.g., "1m", "5m", "1h")
     * @param start Start of time range (ISO-8601 format)
     * @param end End of time range (ISO-8601 format)
     * @return ResponseEntity with list of matching events
     */
    @GetMapping("/symbol/{symbol}/timeframe/{timeframe}")
    public ResponseEntity<List<CoreMarketEvent>> getEventsBySymbolAndTimeframe(
            @PathVariable String symbol,
            @PathVariable String timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        try {
            log.info("Retrieving market events for symbol: {}, timeframe: {} between {} and {}", 
                    symbol, timeframe, start, end);
            
            List<CoreMarketEvent> events = coreMarketEventRepository
                    .findBySymbolAndTimeframeAndIngestedTsBetweenOrderByIngestedTsDesc(
                            symbol, timeframe, start, end);
            
            log.info("Found {} market events for symbol: {}, timeframe: {}", 
                    events.size(), symbol, timeframe);
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            log.error("Error retrieving market events for symbol: {}, timeframe: {}", 
                    symbol, timeframe, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Updates an existing market event.
     * 
     * PUT /api/v1/market-events/{id}
     * 
     * @param id The event ID
     * @param event The updated event data
     * @return ResponseEntity with the updated event
     */
    @PutMapping("/{id}")
    public ResponseEntity<CoreMarketEvent> updateEvent(
            @PathVariable String id, 
            @RequestBody CoreMarketEvent event) {
        try {
            log.info("Updating market event with ID: {}", id);
            
            Optional<CoreMarketEvent> existingEvent = coreMarketEventRepository.findById(id);
            if (existingEvent.isEmpty()) {
                log.warn("Market event not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            event.setId(id);
            CoreMarketEvent updatedEvent = coreMarketEventRepository.save(event);
            
            log.info("Successfully updated market event with ID: {}", id);
            return ResponseEntity.ok(updatedEvent);
            
        } catch (Exception e) {
            log.error("Error updating market event with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Deletes a market event by ID.
     * 
     * DELETE /api/v1/market-events/{id}
     * 
     * @param id The event ID
     * @return ResponseEntity with no content or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        try {
            log.info("Deleting market event with ID: {}", id);
            
            if (!coreMarketEventRepository.existsById(id)) {
                log.warn("Market event not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            coreMarketEventRepository.deleteById(id);
            log.info("Successfully deleted market event with ID: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error deleting market event with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint for market events service.
     * 
     * GET /api/v1/market-events/health
     * 
     * @return ResponseEntity with service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long count = coreMarketEventRepository.count();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "CoreMarketEventService");
            health.put("totalEvents", count);
            health.put("timestamp", Instant.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Health check failed", e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("service", "CoreMarketEventService");
            health.put("error", e.getMessage());
            health.put("timestamp", Instant.now());
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
