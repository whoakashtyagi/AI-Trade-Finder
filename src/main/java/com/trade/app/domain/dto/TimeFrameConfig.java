package com.trade.app.domain.dto;

import com.trade.app.datasource.model.DataSourceConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration for a specific timeframe in AI workflow requests.
 * 
 * Defines which data sources to include and their configurations
 * for a particular timeframe analysis.
 * 
 * This new design supports multiple data sources per timeframe,
 * enabling flexible and extensible data retrieval.
 * 
 * @author AI Trade Finder Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeFrameConfig {
    
    /**
     * List of data source configurations for this timeframe.
     * Each configuration specifies a data source type, time range, and filters.
     * 
     * Example:
     * - CoreMarketEvent data from 9:30 AM to 4:00 PM
     * - OHLC data from 9:00 AM to 4:30 PM
     */
    private List<DataSourceConfig> dataSources;
    
    /**
     * Whether to include data for this timeframe.
     * If false, all data sources will be skipped.
     */
    @Builder.Default
    private boolean enabled = true;
}
