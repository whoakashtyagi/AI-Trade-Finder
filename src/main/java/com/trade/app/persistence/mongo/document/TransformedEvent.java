package com.trade.app.persistence.mongo.document;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB document representing transformed market events.
 * 
 * This document stores market events that have been processed and transformed
 * from raw events, enriched with additional metadata, feature masks, and action codes
 * for downstream processing and trading signals.
 * 
 * @author AI Trade Finder Team
 */
@Document("transformed_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformedEvent {

    /**
     * Unique identifier for the transformed event.
     */
    @Id
    private String id;

    /**
     * Trading symbol (e.g., "AAPL", "TSLA", "NQ").
     * Indexed for efficient symbol-based queries.
     */
    @Indexed
    @Field("symbol")
    private String symbol;

    /**
     * Unique event code identifying the type of event.
     * Indexed for efficient event-type queries.
     */
    @Indexed
    @Field("unique_event_code")
    private String uniqueEventCode;

    /**
     * Timeframe for the market data (e.g., "1m", "5m", "1h", "1d").
     * Indexed for efficient timeframe-based queries.
     */
    @Indexed
    @Field("timeframe")
    private String timeframe;

    /**
     * Timestamp of the actual market event.
     * Indexed for efficient time-range queries.
     */
    @Indexed
    @Field("event_ts")
    private Instant eventTs;

    /**
     * Reference to the original source event that was transformed.
     */
    @Field("source_event_id")
    private String sourceEventId;

    /**
     * Raw event data as received from source.
     */
    @Field("raw_event")
    private String rawEvent;

    /**
     * Short code for the indicator that generated this event (e.g., "RSI", "MACD").
     */
    @Field("indicator_short_code")
    private String indicatorShortCode;

    /**
     * Direction code indicating market direction (e.g., "UP", "DOWN", "NEUTRAL").
     */
    @Field("direction_code")
    private String directionCode;

    /**
     * Action code indicating the recommended action (e.g., "BUY", "SELL", "HOLD").
     */
    @Field("action_code")
    private String actionCode;

    /**
     * Feature mask encoded in base-36 format.
     * Compact representation of active features/flags.
     */
    @Field("feature_mask_base36")
    private String featureMaskBase36;

    /**
     * Numeric value of the feature mask for programmatic access.
     */
    @Field("feature_mask_value")
    private Long featureMaskValue;

    /**
     * Additional extra information as key-value pairs.
     */
    @Field("extras")
    private Map<String, String> extras;

    /**
     * Human-readable description of the unique event code.
     */
    @Field("uec_description")
    private String uecDescription;

    /**
     * Timestamp when this transformed event was created.
     * Defaults to current time.
     */
    @Field("created_ts")
    @Builder.Default
    private Instant createdTs = Instant.now();

    /**
     * Flag indicating if text notification should be sent.
     */
    @JsonAlias({ "isTextNotify", "is_text_notify", "text_notify", "textNotify" })
    @Field("is_text_notify")
    private boolean isTextNotify;

    /**
     * Flag indicating if call notification should be sent.
     */
    @JsonAlias({ "isCallNotify", "is_call_notify", "call_notify", "callNotify" })
    @Field("is_call_notify")
    private boolean isCallNotify;

    /**
     * Flag indicating if Alexa announcement should be made.
     */
    @JsonAlias({ "isAlexaAnnounce", "is_alexa_announce", "alexa_announce", "alexaAnnounce" })
    @Field("is_alexa_announce")
    private boolean isAlexaAnnounce;

    /**
     * Flag indicating if this is a trade signal.
     */
    @JsonAlias({ "isTradeSignal", "is_trade_signal", "trade_signal", "tradeSignal" })
    @Field("is_trade_signal")
    private boolean isTradeSignal;

    /**
     * Flag indicating if reasoner should be triggered.
     */
    @JsonAlias({ "isTriggerReasoner", "is_trigger_reasoner", "trigger_reasoner", "triggerReasoner" })
    @Field("is_trigger_reasoner")
    private boolean isTriggerReasoner;

    /**
     * Message to be announced (for Alexa or other voice systems).
     */
    @Field("announce_msg")
    private String announceMsg;

    /**
     * Message for text notification.
     */
    @Field("txt_notify_msg")
    private String txtNotifyMsg;

    /**
     * Message for call notification.
     */
    @Field("call_notify_msg")
    private String callNotifyMsg;

    /**
     * Approximate price at the time of the event.
     */
    @Field("approx_price_at_event")
    private BigDecimal approxPriceAtEvent;
}
