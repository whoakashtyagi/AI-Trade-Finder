package com.trade.app.datasource.model;

/**
 * Enumeration of available data source types for AI workflow analysis.
 * 
 * Each data source type represents a different category of market data
 * that can be included in AI analysis requests.
 * 
 * @author AI Trade Finder Team
 */
public enum DataSourceType {
    
    /**
     * Core market events from various indicators (RSI, MACD, etc.).
     * Contains processed event signals with indicator metadata.
     */
    CORE_MARKET_EVENT("core_market_event", "Core Market Events"),
    
    /**
     * OHLC (Open, High, Low, Close) candlestick data.
     * Raw price action data at various timeframes.
     */
    OHLC("ohlc", "OHLC Data"),
    
    /**
     * Volume profile data.
     * Volume distribution across price levels.
     */
    VOLUME_PROFILE("volume_profile", "Volume Profile"),
    
    /**
     * Order book depth data.
     * Bid/ask levels and liquidity information.
     */
    ORDER_BOOK("order_book", "Order Book Data"),
    
    /**
     * Transformed market events with enriched metadata.
     * Contains processed events with action codes, feature masks, and trading signals.
     */
    TRANSFORMED_EVENT("transformed_event", "Transformed Events");
    
    private final String code;
    private final String displayName;
    
    DataSourceType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get DataSourceType from code string.
     * 
     * @param code The data source code
     * @return DataSourceType enum value
     * @throws IllegalArgumentException if code is not recognized
     */
    public static DataSourceType fromCode(String code) {
        for (DataSourceType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown data source type: " + code);
    }
}
