package com.trade.app.openai.schema;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured output schema for trade signal identification.
 * 
 * This class defines the JSON schema for trade signals that will be
 * extracted from OpenAI responses using structured outputs.
 * 
 * The AI model will return data conforming to this schema, eliminating
 * the need for manual JSON parsing and validation.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("A trade signal identified through AI analysis of market data")
public class TradeSignalSchema {
    
    @JsonPropertyDescription("The trading symbol or asset (e.g., BTCUSDT, EURUSD)")
    @Schema(description = "Trading symbol or asset", example = "BTCUSDT", required = true)
    private String symbol;
    
    @JsonPropertyDescription("Trade direction: LONG or SHORT")
    @Schema(description = "Trade direction", allowableValues = {"LONG", "SHORT"}, required = true)
    private String direction;
    
    @JsonPropertyDescription("Recommended entry price level")
    @Schema(description = "Entry price level", minimum = "0", required = true)
    private Double entryPrice;
    
    @JsonPropertyDescription("Stop loss price level for risk management")
    @Schema(description = "Stop loss price level", minimum = "0", required = true)
    private Double stopLoss;
    
    @ArraySchema(
        schema = @Schema(description = "Target price level", minimum = "0"),
        minItems = 1,
        maxItems = 5
    )
    @JsonPropertyDescription("Target price levels for profit taking (in order of priority)")
    private List<Double> targets;
    
    @JsonPropertyDescription("Confidence score from 0 to 100, where 100 is maximum confidence")
    @Schema(description = "Confidence score", minimum = "0", maximum = "100", required = true)
    private Integer confidence;
    
    @JsonPropertyDescription("Trading timeframe (e.g., 1h, 4h, 1d)")
    @Schema(description = "Trading timeframe", example = "4h", required = true)
    private String timeframe;
    
    @JsonPropertyDescription("Detailed reasoning and narrative explaining the trade setup")
    @Schema(description = "Trade reasoning and narrative", required = true, minLength = 50)
    private String narrative;
    
    @JsonPropertyDescription("Risk-to-reward ratio for this trade")
    @Schema(description = "Risk-to-reward ratio", minimum = "0", example = "2.5")
    private Double riskRewardRatio;
    
    @JsonPropertyDescription("Key technical indicators supporting this trade")
    @ArraySchema(
        schema = @Schema(description = "Technical indicator name"),
        maxItems = 10
    )
    private List<String> supportingIndicators;
    
    @JsonPropertyDescription("Identified market structure (trend, range, breakout, reversal)")
    @Schema(description = "Market structure", allowableValues = {"trend", "range", "breakout", "reversal"})
    private String marketStructure;
    
    @JsonPropertyDescription("Trade urgency level: low, medium, high")
    @Schema(description = "Trade urgency", allowableValues = {"low", "medium", "high"})
    private String urgency;
    
    @JsonPropertyDescription("Estimated trade duration in hours")
    @Schema(description = "Estimated duration in hours", minimum = "0")
    private Integer estimatedDurationHours;
}
