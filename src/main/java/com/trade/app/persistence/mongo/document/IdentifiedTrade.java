package com.trade.app.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * MongoDB document representing an identified trading opportunity.
 * 
 * This document stores trade setups identified by the AI Trade Finder service.
 * It includes entry zones, stop placement, targets, and confidence levels,
 * along with deduplication mechanisms to prevent duplicate alerts.
 * 
 * @author AI Trade Finder Team
 */
@Document("identified_trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentifiedTrade {

    /**
     * Unique identifier for the identified trade.
     */
    @Id
    private String id;

    /**
     * Trading symbol (e.g., "NQ", "ES", "YM", "GC", "RTY").
     * Indexed for efficient symbol-based queries.
     */
    @Indexed
    @Field("symbol")
    private String symbol;

    /**
     * Trade direction: LONG or SHORT.
     * Indexed for filtering trades by direction.
     */
    @Indexed
    @Field("direction")
    private String direction;

    /**
     * Timestamp when the AI identified this trade opportunity.
     * Indexed for time-based queries and sorting.
     */
    @Indexed
    @Field("identified_at")
    private Instant identifiedAt;

    /**
     * AI confidence level (0-100).
     * Higher values indicate stronger conviction in the trade setup.
     */
    @Field("confidence")
    private Integer confidence;

    /**
     * Current status of the trade.
     * Possible values: IDENTIFIED, ALERTED, EXPIRED, TAKEN, INVALIDATED
     * Indexed for filtering active trades.
     */
    @Indexed
    @Field("status")
    private String status;

    // ========== Entry Details ==========

    /**
     * Type of entry zone (e.g., "FVG_CE", "IFVG", "OB", "BREAKER").
     */
    @Field("entry_zone_type")
    private String entryZoneType;

    /**
     * Entry zone range (e.g., "21780-21800").
     */
    @Field("entry_zone")
    private String entryZone;

    /**
     * Specific entry price recommendation (CE or mid-zone price).
     */
    @Field("entry_price")
    private BigDecimal entryPrice;

    /**
     * Stop loss placement description (e.g., "Below 21750").
     */
    @Field("stop_placement")
    private String stopPlacement;

    /**
     * Target descriptions (e.g., ["POC", "Asia high", "Previous day high"]).
     */
    @Field("targets")
    private List<String> targets;

    /**
     * Risk-reward ratio hint (e.g., "2.5:1", "3:1").
     */
    @Field("rr_hint")
    private String rrHint;

    // ========== Context ==========

    /**
     * AI-generated narrative explaining the trade setup.
     */
    @Field("narrative")
    private String narrative;

    /**
     * List of trigger conditions for entry.
     */
    @Field("trigger_conditions")
    private List<String> triggerConditions;

    /**
     * List of invalidation conditions (when to skip the trade).
     */
    @Field("invalidations")
    private List<String> invalidations;

    /**
     * Session label when trade was identified (e.g., "NY_AM", "LONDON", "ASIA").
     */
    @Field("session_label")
    private String sessionLabel;

    /**
     * Timeframe for the trade setup (e.g., "5m", "15m", "1h").
     */
    @Field("timeframe")
    private String timeframe;

    // ========== Deduplication ==========

    /**
     * Unique deduplication key to prevent duplicate alerts.
     * Format: {symbol}_{direction}_{entryZone}_{YYYYMMDD_HH}
     * Indexed with unique constraint.
     */
    @Indexed(unique = true)
    @Field("dedupe_key")
    private String dedupeKey;

    // ========== Alert Tracking ==========

    /**
     * Flag indicating if alert has been sent.
     */
    @Field("alert_sent")
    private boolean alertSent;

    /**
     * Timestamp when alert was sent.
     */
    @Field("alert_sent_at")
    private Instant alertSentAt;

    /**
     * Type of alert sent (e.g., "CALL", "SMS", "TELEGRAM", "CALL_SMS_TELEGRAM").
     */
    @Field("alert_type")
    private String alertType;

    // ========== Timestamps ==========

    /**
     * Timestamp when this record was created.
     */
    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Timestamp when this trade setup expires and should no longer be considered.
     */
    @Field("expires_at")
    private Instant expiresAt;

    /**
     * Timestamp when this record was last updated.
     */
    @Field("updated_at")
    private Instant updatedAt;

    // ========== Additional Context ==========

    /**
     * Reference to the AI request ID that generated this trade.
     */
    @Field("ai_request_id")
    private String aiRequestId;

    /**
     * Full AI response for audit/debugging purposes.
     */
    @Field("ai_full_response")
    private String aiFullResponse;

    /**
     * Key levels context at the time of identification.
     */
    @Field("key_levels_snapshot")
    private String keyLevelsSnapshot;
}
