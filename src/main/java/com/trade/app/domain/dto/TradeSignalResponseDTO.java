package com.trade.app.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for AI Trade Finder response.
 * 
 * Represents the structured trade signal output from the AI model,
 * including entry zones, targets, stops, and confidence levels.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradeSignalResponseDTO {

    /**
     * Overall status of the analysis.
     * Values: "TRADE_IDENTIFIED", "NO_SETUP", "INSUFFICIENT_DATA", "ERROR"
     */
    @JsonProperty("status")
    private String status;

    /**
     * Trade direction: "LONG" or "SHORT".
     */
    @JsonProperty("direction")
    private String direction;

    /**
     * Symbol for the trade.
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * Primary timeframe for the trade setup.
     */
    @JsonProperty("timeframe")
    private String timeframe;

    /**
     * AI confidence level (0-100).
     */
    @JsonProperty("confidence")
    private Integer confidence;

    /**
     * Entry zone details.
     */
    @JsonProperty("entry")
    private EntryInfo entry;

    /**
     * Stop loss details.
     */
    @JsonProperty("stop")
    private StopInfo stop;

    /**
     * Target levels.
     */
    @JsonProperty("targets")
    private List<TargetInfo> targets;

    /**
     * Risk-reward ratio.
     */
    @JsonProperty("risk_reward")
    private String riskReward;

    /**
     * Narrative explaining the trade setup.
     */
    @JsonProperty("narrative")
    private String narrative;

    /**
     * Key trigger conditions for entry.
     */
    @JsonProperty("trigger_conditions")
    private List<String> triggerConditions;

    /**
     * Invalidation conditions (when to avoid the trade).
     */
    @JsonProperty("invalidations")
    private List<String> invalidations;

    /**
     * Session context.
     */
    @JsonProperty("session_label")
    private String sessionLabel;

    /**
     * Timestamp of analysis.
     */
    @JsonProperty("analysis_timestamp")
    private String analysisTimestamp;

    /**
     * Additional notes or warnings.
     */
    @JsonProperty("notes")
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EntryInfo {
        /**
         * Entry zone type (e.g., "FVG_CE", "IFVG", "OB", "BREAKER").
         */
        @JsonProperty("zone_type")
        private String zoneType;

        /**
         * Entry zone range (e.g., "21780-21800").
         */
        @JsonProperty("zone")
        private String zone;

        /**
         * Specific entry price (CE or mid-zone).
         */
        @JsonProperty("price")
        private BigDecimal price;

        /**
         * Entry method description.
         */
        @JsonProperty("method")
        private String method;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StopInfo {
        /**
         * Stop loss placement description.
         */
        @JsonProperty("placement")
        private String placement;

        /**
         * Specific stop price.
         */
        @JsonProperty("price")
        private BigDecimal price;

        /**
         * Stop reasoning.
         */
        @JsonProperty("reasoning")
        private String reasoning;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TargetInfo {
        /**
         * Target level name (e.g., "POC", "Asia High").
         */
        @JsonProperty("level")
        private String level;

        /**
         * Target price.
         */
        @JsonProperty("price")
        private BigDecimal price;

        /**
         * Target description.
         */
        @JsonProperty("description")
        private String description;
    }
}
