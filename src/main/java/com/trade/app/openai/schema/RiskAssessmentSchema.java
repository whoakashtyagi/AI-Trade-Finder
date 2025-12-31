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
 * Structured output schema for risk assessment.
 * 
 * This class defines the JSON schema for risk assessment analysis
 * that will be extracted from OpenAI responses using structured outputs.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("Risk assessment for a trading opportunity")
public class RiskAssessmentSchema {
    
    @JsonPropertyDescription("Overall risk level: very_low, low, medium, high, very_high")
    @Schema(description = "Risk level", allowableValues = {"very_low", "low", "medium", "high", "very_high"}, required = true)
    private String riskLevel;
    
    @JsonPropertyDescription("Risk score from 0 to 100, where 100 is maximum risk")
    @Schema(description = "Risk score", minimum = "0", maximum = "100", required = true)
    private Integer riskScore;
    
    @JsonPropertyDescription("Recommended position size as percentage of portfolio")
    @Schema(description = "Position size percentage", minimum = "0", maximum = "100", required = true)
    private Double positionSizePercent;
    
    @JsonPropertyDescription("Maximum acceptable loss in percentage")
    @Schema(description = "Maximum loss percentage", minimum = "0", maximum = "100", required = true)
    private Double maxLossPercent;
    
    @JsonPropertyDescription("Probability of success from 0 to 100")
    @Schema(description = "Win probability", minimum = "0", maximum = "100", required = true)
    private Integer winProbability;
    
    @JsonPropertyDescription("Expected risk-to-reward ratio")
    @Schema(description = "Risk-reward ratio", minimum = "0", required = true)
    private Double riskRewardRatio;
    
    @JsonPropertyDescription("Identified risk factors")
    @ArraySchema(
        schema = @Schema(description = "Risk factor description"),
        minItems = 1,
        maxItems = 10
    )
    private List<String> riskFactors;
    
    @JsonPropertyDescription("Risk mitigation strategies")
    @ArraySchema(
        schema = @Schema(description = "Mitigation strategy"),
        minItems = 1,
        maxItems = 10
    )
    private List<String> mitigationStrategies;
    
    @JsonPropertyDescription("Market conditions affecting risk")
    @ArraySchema(
        schema = @Schema(description = "Market condition"),
        maxItems = 10
    )
    private List<String> marketConditions;
    
    @JsonPropertyDescription("Detailed risk assessment narrative")
    @Schema(description = "Risk narrative", required = true, minLength = 50)
    private String narrative;
    
    @JsonPropertyDescription("Should this trade be taken: yes, no, maybe")
    @Schema(description = "Trade recommendation", allowableValues = {"yes", "no", "maybe"}, required = true)
    private String recommendation;
    
    @JsonPropertyDescription("Additional notes and warnings")
    @Schema(description = "Additional notes")
    private String notes;
}
