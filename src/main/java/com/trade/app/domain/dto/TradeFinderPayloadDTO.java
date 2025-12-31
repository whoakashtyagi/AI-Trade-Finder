package com.trade.app.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for AI Trade Finder payload.
 * 
 * This payload is sent to the AI model to analyze market conditions
 * and identify high-confidence trading opportunities.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradeFinderPayloadDTO {

    /**
     * Metadata about the request context.
     */
    @JsonProperty("meta")
    private MetaInfo meta;

    /**
     * Analysis profile to use (e.g., "SILVER_BULLET_WINDOW", "FULL_SESSION").
     */
    @JsonProperty("analysis_profile")
    private String analysisProfile;

    /**
     * Task description for the AI.
     */
    @JsonProperty("task")
    private String task;

    /**
     * Stream of transformed events.
     */
    @JsonProperty("event_stream")
    private List<EventInfo> eventStream;

    /**
     * OHLC candle context for price action analysis.
     */
    @JsonProperty("ohlc_context")
    private Map<String, List<CandleInfo>> ohlcContext;

    /**
     * Daily trading data context (session opens, closes, ranges).
     */
    @JsonProperty("daily_context")
    private DailyContextInfo dailyContext;

    /**
     * Manual key levels from Google Sheets.
     */
    @JsonProperty("key_levels")
    private List<KeyLevelInfo> keyLevels;

    /**
     * Knowledge base snippets for indicator context.
     */
    @JsonProperty("kb_snippets")
    private Map<String, String> kbSnippets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaInfo {
        @JsonProperty("symbol")
        private String symbol;

        @JsonProperty("date")
        private String date;

        @JsonProperty("now_ts")
        private String nowTs;

        @JsonProperty("session_label")
        private String sessionLabel;

        @JsonProperty("run_context")
        private String runContext;

        @JsonProperty("requested_timeframes")
        private List<String> requestedTimeframes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventInfo {
        @JsonProperty("ts")
        private String ts;

        @JsonProperty("indicator")
        private String indicator;

        @JsonProperty("indicator_short_code")
        private String indicatorShortCode;

        @JsonProperty("category")
        private String category;

        @JsonProperty("direction")
        private String direction;

        @JsonProperty("tf")
        private String timeframe;

        @JsonProperty("price")
        private String price;

        @JsonProperty("details")
        private String details;

        @JsonProperty("action_code")
        private String actionCode;

        @JsonProperty("uec")
        private String uec;

        @JsonProperty("is_trigger_reasoner")
        private Boolean isTriggerReasoner;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandleInfo {
        @JsonProperty("ts")
        private String timestamp;

        @JsonProperty("open")
        private String open;

        @JsonProperty("high")
        private String high;

        @JsonProperty("low")
        private String low;

        @JsonProperty("close")
        private String close;

        @JsonProperty("volume")
        private Long volume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyContextInfo {
        @JsonProperty("day_open")
        private String dayOpen;

        @JsonProperty("midnight_open")
        private String midnightOpen;

        @JsonProperty("open_0830")
        private String open0830;

        @JsonProperty("open_0930")
        private String open0930;

        @JsonProperty("open_1000")
        private String open1000;

        @JsonProperty("weekly_open")
        private String weeklyOpen;

        @JsonProperty("previous_day_close")
        private String previousDayClose;

        @JsonProperty("prev_day_4pm_close")
        private String prevDay4pmClose;

        @JsonProperty("day_close")
        private String dayClose;

        @JsonProperty("day_high")
        private String dayHigh;

        @JsonProperty("day_low")
        private String dayLow;

        @JsonProperty("day_high_time")
        private String dayHighTime;

        @JsonProperty("day_low_time")
        private String dayLowTime;

        @JsonProperty("asia_range")
        private String asiaRange;

        @JsonProperty("london_range")
        private String londonRange;

        @JsonProperty("ny_am_range")
        private String nyAmRange;

        @JsonProperty("ny_lunch_range")
        private String nyLunchRange;

        @JsonProperty("ny_pm_range")
        private String nyPmRange;

        @JsonProperty("day_range")
        private String dayRange;

        @JsonProperty("manual_additional_data")
        private Map<String, String> manualAdditionalData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyLevelInfo {
        @JsonProperty("type")
        private String type;

        @JsonProperty("key")
        private String key;

        @JsonProperty("value")
        private String value;

        @JsonProperty("description")
        private String description;
    }
}
