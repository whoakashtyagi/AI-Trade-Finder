package com.trade.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for trade finder statistics.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeStatisticsDTO {
    
    /**
     * Number of hours the statistics cover.
     */
    private Integer periodHours;
    
    /**
     * Total number of trades in the period.
     */
    private Long totalTrades;
    
    /**
     * Number of active trades.
     */
    private Long activeTrades;
    
    /**
     * Number of expired trades.
     */
    private Long expiredTrades;
    
    /**
     * Number of alerted trades.
     */
    private Long alertedTrades;
    
    /**
     * Average confidence score.
     */
    private Double averageConfidence;
    
    /**
     * Trade count by symbol.
     */
    private Map<String, Long> bySymbol;
    
    /**
     * Trade count by direction (LONG/SHORT).
     */
    private Map<String, Long> byDirection;
    
    /**
     * Timestamp of statistics generation.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
}
