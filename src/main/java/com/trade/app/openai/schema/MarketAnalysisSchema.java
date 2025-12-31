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
 * Structured output schema for market analysis.
 * 
 * This class defines the JSON schema for comprehensive market analysis
 * that will be extracted from OpenAI responses using structured outputs.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("Comprehensive market analysis with trend identification and key levels")
public class MarketAnalysisSchema {
    
    @JsonPropertyDescription("The analyzed trading symbol or asset")
    @Schema(description = "Trading symbol", example = "BTCUSDT", required = true)
    private String symbol;
    
    @JsonPropertyDescription("Current timeframe being analyzed")
    @Schema(description = "Analysis timeframe", example = "4h", required = true)
    private String timeframe;
    
    @JsonPropertyDescription("Overall market trend: bullish, bearish, neutral, or sideways")
    @Schema(description = "Market trend", allowableValues = {"bullish", "bearish", "neutral", "sideways"}, required = true)
    private String trend;
    
    @JsonPropertyDescription("Trend strength from 0 to 100")
    @Schema(description = "Trend strength score", minimum = "0", maximum = "100", required = true)
    private Integer trendStrength;
    
    @JsonPropertyDescription("Market sentiment: fear, greed, neutral")
    @Schema(description = "Market sentiment", allowableValues = {"fear", "greed", "neutral"})
    private String sentiment;
    
    @JsonPropertyDescription("Current volatility level: low, medium, high")
    @Schema(description = "Volatility level", allowableValues = {"low", "medium", "high"}, required = true)
    private String volatility;
    
    @ArraySchema(
        schema = @Schema(description = "Support price level", minimum = "0"),
        minItems = 1,
        maxItems = 5
    )
    @JsonPropertyDescription("Key support levels in ascending order")
    private List<Double> supportLevels;
    
    @ArraySchema(
        schema = @Schema(description = "Resistance price level", minimum = "0"),
        minItems = 1,
        maxItems = 5
    )
    @JsonPropertyDescription("Key resistance levels in ascending order")
    private List<Double> resistanceLevels;
    
    @JsonPropertyDescription("Detailed market analysis narrative")
    @Schema(description = "Market analysis narrative", required = true, minLength = 100)
    private String analysis;
    
    @JsonPropertyDescription("Key observations and findings from the analysis")
    @ArraySchema(
        schema = @Schema(description = "Market observation"),
        minItems = 1,
        maxItems = 10
    )
    private List<String> keyObservations;
    
    @JsonPropertyDescription("Potential risk factors and concerns")
    @ArraySchema(
        schema = @Schema(description = "Risk factor"),
        maxItems = 10
    )
    private List<String> riskFactors;
    
    @JsonPropertyDescription("Trading opportunities identified in this analysis")
    @ArraySchema(
        schema = @Schema(description = "Trading opportunity"),
        maxItems = 5
    )
    private List<String> opportunities;
    
    @JsonPropertyDescription("Recommended action: buy, sell, hold, wait")
    @Schema(description = "Recommended action", allowableValues = {"buy", "sell", "hold", "wait"})
    private String recommendation;
    
    @JsonPropertyDescription("Confidence level in this analysis from 0 to 100")
    @Schema(description = "Analysis confidence", minimum = "0", maximum = "100", required = true)
    private Integer confidence;
}
