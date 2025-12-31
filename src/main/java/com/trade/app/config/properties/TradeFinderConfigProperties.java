package com.trade.app.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration properties for AI Trade Finder.
 * 
 * This class provides type-safe access to AI Trade Finder configuration
 * properties defined in application.properties.
 * 
 * @author AI Trade Finder Team
 */
@Configuration
@ConfigurationProperties(prefix = "ai.trade-finder")
@Data
public class TradeFinderConfigProperties {

    /**
     * Enable/disable the AI Trade Finder scheduled job.
     */
    private boolean enabled = true;

    /**
     * List of symbols to analyze.
     */
    private List<String> symbols = List.of("NQ", "ES", "YM", "GC", "RTY");

    /**
     * Scheduler interval in milliseconds.
     * Default: 300000 (5 minutes)
     */
    private long intervalMs = 300000L;

    /**
     * How many minutes back to look for events.
     */
    private int eventLookbackMinutes = 90;

    /**
     * Number of OHLC candles to fetch per timeframe.
     */
    private int ohlcCandleCount = 100;

    /**
     * Trade expiry time in hours.
     */
    private int tradeExpiryHours = 4;

    /**
     * System prompt file location.
     */
    private String systemPromptFile = "prompts/trade_finder_system.txt";

    /**
     * Analysis profile to use.
     */
    private String analysisProfile = "SILVER_BULLET_WINDOW";

    /**
     * Confidence threshold for high-confidence trades.
     * High confidence: >= this value -> CALL + SMS + Telegram
     */
    private int confidenceThresholdHigh = 80;

    /**
     * Confidence threshold for medium-confidence trades.
     * Medium confidence: >= this value -> SMS + Telegram
     */
    private int confidenceThresholdMedium = 60;

    /**
     * Phone numbers for alerts (optional).
     */
    private AlertConfig alert = new AlertConfig();

    @Data
    public static class AlertConfig {
        /**
         * Primary phone number for alerts.
         */
        private String phoneNumber;

        /**
         * Telegram chat ID for alerts.
         */
        private String telegramChatId;

        /**
         * Enable/disable phone call alerts.
         */
        private boolean callEnabled = true;

        /**
         * Enable/disable SMS alerts.
         */
        private boolean smsEnabled = true;

        /**
         * Enable/disable Telegram alerts.
         */
        private boolean telegramEnabled = true;
    }
}
