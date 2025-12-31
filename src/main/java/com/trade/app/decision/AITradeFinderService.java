package com.trade.app.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.app.domain.dto.TradeFinderPayloadDTO;
import com.trade.app.domain.dto.TradeSignalResponseDTO;
import com.trade.app.openai.client.AIClientService;
import com.trade.app.openai.dto.AIRequestDTO;
import com.trade.app.openai.dto.AIResponseDTO;
import com.trade.app.openai.exception.AIClientException;
import com.trade.app.persistence.mongo.document.IdentifiedTrade;
import com.trade.app.persistence.mongo.document.TransformedEvent;
import com.trade.app.persistence.mongo.document.OHLCData;
import com.trade.app.persistence.mongo.repository.IdentifiedTradeRepository;
import com.trade.app.persistence.mongo.repository.TransformedEventRepository;
import com.trade.app.persistence.mongo.repository.OHLCRepository;
import com.trade.app.service.OperationLogService;
import com.trade.app.util.OperationLogContext;
import com.trade.app.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Trade Finder Service.
 * 
 * This service runs on a scheduled basis to analyze market events,
 * OHLC data, and session context to identify high-confidence trading
 * opportunities using AI-powered analysis.
 * 
 * Features:
 * - Query recent transformed events from MongoDB
 * - Query OHLC candle data across multiple timeframes
 * - Build comprehensive JSON payload for AI analysis
 * - Parse AI responses for trade signals
 * - Deduplication mechanism to prevent duplicate alerts
 * - Confidence-based alert dispatching
 * 
 * @author AI Trade Finder Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AITradeFinderService {

    // ========== Dependencies ==========
    
    private final TransformedEventRepository transformedEventRepository;
    private final IdentifiedTradeRepository identifiedTradeRepository;
    private final OHLCRepository ohlcRepository;
    private final AIClientService aiClientService;
    private final ObjectMapper objectMapper;
    private final com.trade.app.util.PromptLoader promptLoader;
    private final OperationLogService operationLogService;

    // ========== Configuration ==========
    
    @Value("${ai.trade-finder.enabled:true}")
    private boolean enabled;

    @Value("${ai.trade-finder.symbols:NQ,ES,YM,GC,RTY}")
    private String symbolsConfig;

    @Value("${ai.trade-finder.event-lookback-minutes:90}")
    private int eventLookbackMinutes;

    @Value("${ai.trade-finder.ohlc-candle-count:100}")
    private int ohlcCandleCount;

    @Value("${ai.trade-finder.trade-expiry-hours:4}")
    private int tradeExpiryHours;

    @Value("${ai.trade-finder.system-prompt-file:prompts/trade_finder_system.txt}")
    private String systemPromptFile;

    @Value("${ai.trade-finder.analysis-profile:SILVER_BULLET_WINDOW}")
    private String analysisProfile;

    @Value("${ai.trade-finder.confidence-threshold-high:80}")
    private int confidenceThresholdHigh;

    @Value("${ai.trade-finder.confidence-threshold-medium:60}")
    private int confidenceThresholdMedium;

    // Time zone for trading sessions
    private static final ZoneId NY_TIMEZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    // ========== Scheduled Job ==========

    /**
     * Main scheduler that runs every 5 minutes to find trades.
     * Configurable via cron expression in application.properties.
     */
    @Scheduled(fixedRateString = "${ai.trade-finder.interval-ms:300000}") // Default: 5 minutes
    public void findTrades() {
        if (!enabled) {
            log.debug("AI Trade Finder is disabled");
            return;
        }

        String operationId = operationLogService.startOrReuse(
            "TRADE_FINDER_RUN",
            "AI Trade Finder run",
            "SCHEDULER",
            Map.of("symbols", symbolsConfig)
        );

        log.info("=== AI Trade Finder: Starting scheduled run ===");
        long startTime = System.currentTimeMillis();

        List<String> symbols = parseSymbols();
        int tradesFound = 0;

        for (String symbol : symbols) {
            try {
                log.info("Analyzing symbol: {}", symbol);
                boolean tradeFound = findTradesForSymbol(symbol);
                if (tradeFound) {
                    tradesFound++;
                }
            } catch (Exception e) {
                log.error("Error analyzing symbol {}: {}", symbol, e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("=== AI Trade Finder: Completed in {}ms. Trades found: {} ===", duration, tradesFound);

        operationLogService.completeSuccess(operationId, "Trade finder run completed", Map.of(
            "durationMs", duration,
            "tradesFound", tradesFound
        ));
        OperationLogContext.clear();
    }

    // ========== Core Logic ==========

    /**
     * Finds trades for a specific symbol.
     * 
     * @param symbol The trading symbol
     * @return true if a trade was identified, false otherwise
     */
    private boolean findTradesForSymbol(String symbol) {
        try {
            // Step 1: Build payload with recent events and market data
            TradeFinderPayloadDTO payload = buildPayload(symbol);

            // Step 2: Call AI with payload
            TradeSignalResponseDTO aiResponse = callAI(payload);

            // Step 3: Check if trade was identified
            if (aiResponse == null || !"TRADE_IDENTIFIED".equals(aiResponse.getStatus())) {
                log.debug("No trade identified for {}: {}", symbol, 
                    aiResponse != null ? aiResponse.getStatus() : "NULL_RESPONSE");
                return false;
            }

            // Step 4: Save and alert
            return saveAndAlert(symbol, aiResponse);

        } catch (Exception e) {
            log.error("Error in findTradesForSymbol for {}: {}", symbol, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Builds the AI payload with events, OHLC, and context data.
     * 
     * @param symbol The trading symbol
     * @return TradeFinderPayloadDTO ready for AI analysis
     */
    private TradeFinderPayloadDTO buildPayload(String symbol) {
        Instant now = Instant.now();
        Instant eventCutoff = now.minus(eventLookbackMinutes, ChronoUnit.MINUTES);

        log.debug("Building payload for {}: looking back {} minutes", symbol, eventLookbackMinutes);

        // Query transformed events
        List<TransformedEvent> events = transformedEventRepository
            .findBySymbolAndEventTsAfterOrderByEventTsDesc(symbol, eventCutoff);

        log.debug("Found {} transformed events for {}", events.size(), symbol);

        // Build event stream
        List<TradeFinderPayloadDTO.EventInfo> eventStream = events.stream()
            .map(this::mapEventToEventInfo)
            .collect(Collectors.toList());

        // Build OHLC context for multiple timeframes
        Map<String, List<TradeFinderPayloadDTO.CandleInfo>> ohlcContext = buildOHLCContext(symbol);

        // Build metadata
        TradeFinderPayloadDTO.MetaInfo meta = TradeFinderPayloadDTO.MetaInfo.builder()
            .symbol(symbol)
            .date(DateTimeFormatter.ISO_LOCAL_DATE.format(now.atZone(NY_TIMEZONE)))
            .nowTs(ISO_FORMATTER.format(now))
            .sessionLabel(determineSessionLabel(now))
            .runContext("SCHEDULED_TRADE_FINDER")
            .requestedTimeframes(Arrays.asList("5m", "15m", "1h", "4h"))
            .build();

        return TradeFinderPayloadDTO.builder()
            .meta(meta)
            .analysisProfile(analysisProfile)
            .task("Analyze recent market events and identify high-confluence trade setup with entry, stop, and targets.")
            .eventStream(eventStream)
            .ohlcContext(ohlcContext)
            .build();
    }

    /**
     * Calls the AI service with the trade finder payload.
     * 
     * @param payload The trade finder payload
     * @return Parsed TradeSignalResponseDTO or null if error
     */
    private TradeSignalResponseDTO callAI(TradeFinderPayloadDTO payload) {
        try {
            // Convert payload to JSON string
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Build AI request
            AIRequestDTO request = AIRequestDTO.builder()
                .input(payloadJson)
                .systemInstructions(loadSystemPrompt())
                .maxTokens(16000)
                .temperature(0.7)
                .requestId("TRADE_FINDER_" + payload.getMeta().getSymbol() + "_" + System.currentTimeMillis())
                .build();

            log.debug("Calling AI service for symbol: {}", payload.getMeta().getSymbol());

            // Call AI
            AIResponseDTO aiResponse = aiClientService.sendReasoningRequest(request);

            // Parse response
            String aiOutput = aiResponse.getOutput();
            if (aiOutput == null || aiOutput.isEmpty()) {
                log.warn("Empty AI response for symbol: {}", payload.getMeta().getSymbol());
                return null;
            }

            // Parse JSON response
            return parseAIResponse(aiOutput);

        } catch (AIClientException e) {
            log.error("AI client error: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Error calling AI: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses the AI response JSON into TradeSignalResponseDTO.
     * 
     * @param aiOutput The raw AI output
     * @return Parsed TradeSignalResponseDTO
     */
    private TradeSignalResponseDTO parseAIResponse(String aiOutput) {
        try {
            // Try to extract JSON if wrapped in markdown code blocks
            String cleanJson = aiOutput.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            return objectMapper.readValue(cleanJson, TradeSignalResponseDTO.class);
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            log.debug("AI Output: {}", aiOutput);
            return null;
        }
    }

    /**
     * Saves the identified trade and sends alerts based on confidence level.
     * 
     * @param symbol The trading symbol
     * @param aiResponse The AI trade signal response
     * @return true if trade was saved and alerted
     */
    private boolean saveAndAlert(String symbol, TradeSignalResponseDTO aiResponse) {
        try {
            // Generate deduplication key
            String entryZone = aiResponse.getEntry() != null ? aiResponse.getEntry().getZone() : "UNKNOWN";
            String dedupeKey = generateDedupeKey(symbol, aiResponse.getDirection(), entryZone, Instant.now());

            // Check if already exists
            if (identifiedTradeRepository.existsByDedupeKey(dedupeKey)) {
                log.info("Duplicate trade setup detected, skipping: {}", dedupeKey);
                return false;
            }

            // Build IdentifiedTrade entity
            IdentifiedTrade trade = buildIdentifiedTrade(symbol, aiResponse, dedupeKey);

            // Save to database
            identifiedTradeRepository.save(trade);
            log.info("Saved identified trade: {} {} at {} with confidence {}",
                trade.getSymbol(), trade.getDirection(), trade.getEntryZone(), trade.getConfidence());

            // Dispatch alerts based on confidence
            dispatchAlerts(trade);

            return true;

        } catch (Exception e) {
            log.error("Error saving and alerting trade: {}", e.getMessage(), e);
            return false;
        }
    }

    // ========== Helper Methods ==========

    /**
     * Generates a deduplication key for trade identification.
     * Format: {symbol}_{direction}_{entryZone}_{YYYYMMDD_HH}
     * 
     * @param symbol The trading symbol
     * @param direction The trade direction
     * @param entryZone The entry zone
     * @param time The current time
     * @return Deduplication key
     */
    private String generateDedupeKey(String symbol, String direction, String entryZone, Instant time) {
        String hourKey = DateTimeFormatter.ofPattern("yyyyMMdd_HH")
            .format(time.atZone(NY_TIMEZONE));
        String cleanZone = entryZone != null ? entryZone.replace("-", "_").replace(" ", "") : "NONE";
        return symbol + "_" + direction + "_" + cleanZone + "_" + hourKey;
    }

    /**
     * Builds an IdentifiedTrade entity from the AI response.
     * 
     * @param symbol The trading symbol
     * @param aiResponse The AI response
     * @param dedupeKey The deduplication key
     * @return IdentifiedTrade entity
     */
    private IdentifiedTrade buildIdentifiedTrade(String symbol, TradeSignalResponseDTO aiResponse, String dedupeKey) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(tradeExpiryHours, ChronoUnit.HOURS);

        TradeSignalResponseDTO.EntryInfo entry = aiResponse.getEntry();
        TradeSignalResponseDTO.StopInfo stop = aiResponse.getStop();

        return IdentifiedTrade.builder()
            .symbol(symbol)
            .direction(aiResponse.getDirection())
            .identifiedAt(now)
            .confidence(aiResponse.getConfidence())
            .status(Constants.TradeStatus.IDENTIFIED)
            .entryZoneType(entry != null ? entry.getZoneType() : null)
            .entryZone(entry != null ? entry.getZone() : null)
            .entryPrice(entry != null ? entry.getPrice() : null)
            .stopPlacement(stop != null ? stop.getPlacement() : null)
            .targets(aiResponse.getTargets() != null ? 
                aiResponse.getTargets().stream().map(t -> t.getLevel()).collect(Collectors.toList()) : null)
            .rrHint(aiResponse.getRiskReward())
            .narrative(aiResponse.getNarrative())
            .triggerConditions(aiResponse.getTriggerConditions())
            .invalidations(aiResponse.getInvalidations())
            .sessionLabel(aiResponse.getSessionLabel())
            .timeframe(aiResponse.getTimeframe())
            .dedupeKey(dedupeKey)
            .alertSent(false)
            .createdAt(now)
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * Dispatches alerts based on confidence level.
     * 
     * @param trade The identified trade
     */
    private void dispatchAlerts(IdentifiedTrade trade) {
        int confidence = trade.getConfidence() != null ? trade.getConfidence() : 0;

        String alertType;
        if (confidence >= confidenceThresholdHigh) {
            // High confidence: CALL + SMS + Telegram
            alertType = Constants.AlertType.CALL_SMS_TELEGRAM;
            log.info("HIGH CONFIDENCE TRADE: {} {} with {}% confidence - Dispatching all alerts",
                trade.getSymbol(), trade.getDirection(), confidence);
            // TODO: Integrate with TwilioService and TelegramService
        } else if (confidence >= confidenceThresholdMedium) {
            // Medium confidence: SMS + Telegram
            alertType = Constants.AlertType.SMS_TELEGRAM;
            log.info("MEDIUM CONFIDENCE TRADE: {} {} with {}% confidence - Dispatching SMS+Telegram",
                trade.getSymbol(), trade.getDirection(), confidence);
            // TODO: Integrate with TwilioService and TelegramService
        } else {
            // Low confidence: Log only
            alertType = Constants.AlertType.LOG_ONLY;
            log.info("LOW CONFIDENCE TRADE: {} {} with {}% confidence - Logging only",
                trade.getSymbol(), trade.getDirection(), confidence);
        }

        // Update trade with alert info
        trade.setAlertSent(true);
        trade.setAlertSentAt(Instant.now());
        trade.setAlertType(alertType);
        identifiedTradeRepository.save(trade);
    }

    /**
     * Maps TransformedEvent to EventInfo DTO.
     * 
     * @param event The transformed event
     * @return EventInfo DTO
     */
    private TradeFinderPayloadDTO.EventInfo mapEventToEventInfo(TransformedEvent event) {
        return TradeFinderPayloadDTO.EventInfo.builder()
            .ts(ISO_FORMATTER.format(event.getEventTs()))
            .indicator(event.getIndicatorShortCode())
            .indicatorShortCode(event.getIndicatorShortCode())
            .category(extractCategory(event.getIndicatorShortCode()))
            .direction(mapDirectionCode(event.getDirectionCode()))
            .timeframe(event.getTimeframe())
            .price(event.getApproxPriceAtEvent() != null ? event.getApproxPriceAtEvent().toString() : null)
            .details(event.getUecDescription())
            .actionCode(event.getActionCode())
            .uec(event.getUniqueEventCode())
            .isTriggerReasoner(event.isTriggerReasoner())
            .build();
    }

    /**
     * Builds OHLC context for multiple timeframes.
     * 
     * @param symbol The trading symbol
     * @return Map of timeframe to candle data
     */
    private Map<String, List<TradeFinderPayloadDTO.CandleInfo>> buildOHLCContext(String symbol) {
        Map<String, List<TradeFinderPayloadDTO.CandleInfo>> context = new HashMap<>();
        List<String> timeframes = Arrays.asList("5m", "15m", "1h", "4h");

        for (String timeframe : timeframes) {
            List<OHLCData> candles = queryOHLCData(symbol, timeframe, ohlcCandleCount);
            List<TradeFinderPayloadDTO.CandleInfo> candleInfos = candles.stream()
                .map(this::mapOHLCToCandleInfo)
                .collect(Collectors.toList());
            context.put(timeframe, candleInfos);
        }

        return context;
    }

    /**
     * Queries OHLC data for a symbol and timeframe.
     * 
     * @param symbol The trading symbol
     * @param timeframe The timeframe
     * @param limit Maximum number of candles
     * @return List of OHLC data
     */
    private List<OHLCData> queryOHLCData(String symbol, String timeframe, int limit) {
        try {
            Instant cutoff = Instant.now().minus(limit * getTimeframeMinutes(timeframe), ChronoUnit.MINUTES);
            List<OHLCData> data = ohlcRepository.findBySymbolAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                symbol, timeframe, cutoff, Instant.now()
            );

            // Limit to requested count
            if (data.size() > limit) {
                data = data.subList(data.size() - limit, data.size());
            }

            return data;
        } catch (Exception e) {
            log.warn("Error querying OHLC data for {} {}: {}", symbol, timeframe, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Maps OHLCData to CandleInfo DTO.
     * 
     * @param ohlc The OHLC data
     * @return CandleInfo DTO
     */
    private TradeFinderPayloadDTO.CandleInfo mapOHLCToCandleInfo(OHLCData ohlc) {
        return TradeFinderPayloadDTO.CandleInfo.builder()
            .timestamp(ISO_FORMATTER.format(ohlc.getTimestamp()))
            .open(ohlc.getOpen() != null ? ohlc.getOpen().toString() : null)
            .high(ohlc.getHigh() != null ? ohlc.getHigh().toString() : null)
            .low(ohlc.getLow() != null ? ohlc.getLow().toString() : null)
            .close(ohlc.getClose() != null ? ohlc.getClose().toString() : null)
            .volume(ohlc.getVolume())
            .build();
    }

    /**
     * Parses the symbols configuration.
     * 
     * @return List of symbols
     */
    private List<String> parseSymbols() {
        return Arrays.stream(symbolsConfig.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Determines the current trading session label.
     * 
     * @param time The current time
     * @return Session label
     */
    private String determineSessionLabel(Instant time) {
        int hour = time.atZone(NY_TIMEZONE).getHour();
        
        if (hour >= 18 || hour < 3) {
            return "ASIA";
        } else if (hour >= 3 && hour < 8) {
            return "LONDON";
        } else if (hour >= 8 && hour < 12) {
            return "NY_AM";
        } else if (hour >= 12 && hour < 14) {
            return "NY_LUNCH";
        } else {
            return "NY_PM";
        }
    }

    /**
     * Extracts category from indicator short code.
     * 
     * @param indicatorCode The indicator code
     * @return Category string
     */
    private String extractCategory(String indicatorCode) {
        if (indicatorCode == null) return "unknown";
        
        String code = indicatorCode.toLowerCase();
        if (code.contains("cisd")) return "cisd";
        if (code.contains("smt")) return "smt";
        if (code.contains("fvg") || code.contains("imbalance")) return "fvg";
        if (code.contains("sweep") || code.contains("liquidity")) return "sweep";
        if (code.contains("rsi")) return "oscillator";
        if (code.contains("macd")) return "oscillator";
        
        return "other";
    }

    /**
     * Maps direction code to standardized format.
     * 
     * @param directionCode The direction code
     * @return Standardized direction (B/S/N)
     */
    private String mapDirectionCode(String directionCode) {
        if (directionCode == null) return "N";
        
        String code = directionCode.toUpperCase();
        if (code.startsWith("B") || code.contains("BULL") || code.contains("UP")) return "B";
        if (code.startsWith("S") || code.contains("BEAR") || code.contains("DOWN")) return "S";
        
        return "N";
    }

    /**
     * Gets timeframe duration in minutes.
     * 
     * @param timeframe The timeframe string
     * @return Duration in minutes
     */
    private int getTimeframeMinutes(String timeframe) {
        if (timeframe == null) return 5;
        
        String tf = timeframe.toLowerCase();
        if (tf.contains("1m")) return 1;
        if (tf.contains("5m")) return 5;
        if (tf.contains("15m")) return 15;
        if (tf.contains("30m")) return 30;
        if (tf.contains("1h") || tf.contains("60m")) return 60;
        if (tf.contains("4h")) return 240;
        if (tf.contains("1d")) return 1440;
        
        return 5;
    }

    /**
     * Loads system prompt from file.
     * 
     * @return System prompt string
     */
    private String loadSystemPrompt() {
        String defaultPrompt = "You are an expert trading AI analyzing market structure. " +
               "Identify high-confidence trade setups based on liquidity sweeps, CISD patterns, and FVG entries. " +
               "Return your analysis as JSON with status, direction, confidence, entry, stop, targets, and narrative.";
        
        return promptLoader.loadPromptWithFallback(systemPromptFile, defaultPrompt);
    }
}
