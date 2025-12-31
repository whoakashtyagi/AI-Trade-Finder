package com.trade.app.util;

/**
 * Application-wide constants.
 * 
 * Centralizes commonly used constant values to avoid magic numbers
 * and strings scattered throughout the codebase.
 * 
 * @author AI Trade Finder Team
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Trade status constants.
     */
    public static final class TradeStatus {
        public static final String IDENTIFIED = "IDENTIFIED";
        public static final String ALERTED = "ALERTED";
        public static final String EXPIRED = "EXPIRED";
        public static final String CANCELLED = "CANCELLED";
        public static final String TAKEN = "TAKEN";
        public static final String INVALIDATED = "INVALIDATED";

        private TradeStatus() {}
    }

    /**
     * Trade direction constants.
     */
    public static final class TradeDirection {
        public static final String LONG = "LONG";
        public static final String SHORT = "SHORT";
        
        private TradeDirection() {}
    }

    /**
     * AI response status constants.
     */
    public static final class AIResponseStatus {
        public static final String COMPLETED = "completed";
        public static final String FAILED = "failed";
        public static final String IN_PROGRESS = "in_progress";
        public static final String CANCELLED = "cancelled";
        
        private AIResponseStatus() {}
    }

    /**
     * AI trade signal response status constants.
     */
    public static final class TradeSignalStatus {
        public static final String TRADE_IDENTIFIED = "TRADE_IDENTIFIED";
        public static final String NO_SETUP = "NO_SETUP";
        public static final String INSUFFICIENT_DATA = "INSUFFICIENT_DATA";
        public static final String ERROR = "ERROR";

        private TradeSignalStatus() {}
    }

    /**
     * Conversation status constants.
     */
    public static final class ConversationStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String COMPLETED = "COMPLETED";
        public static final String EXPIRED = "EXPIRED";

        private ConversationStatus() {}
    }

    /**
     * Conversation type constants.
     */
    public static final class ConversationType {
        public static final String TRADE_FOLLOWUP = "TRADE_FOLLOWUP";
        public static final String TRADE_ANALYSIS = "TRADE_ANALYSIS";
        public static final String MARKET_ANALYSIS = "MARKET_ANALYSIS";
        public static final String WORKFLOW = "WORKFLOW";

        private ConversationType() {}
    }

    /**
     * Entity type constants for conversations.
     */
    public static final class EntityType {
        public static final String TRADE = "TRADE";
        public static final String SYMBOL = "SYMBOL";
        public static final String PATTERN = "PATTERN";

        private EntityType() {}
    }

    /**
     * Entry zone type constants.
     */
    public static final class EntryZoneType {
        public static final String FVG_CE = "FVG_CE";
        public static final String IFVG = "IFVG";
        public static final String OB = "OB";
        public static final String BREAKER = "BREAKER";
        public static final String MITIGATION = "MITIGATION";

        private EntryZoneType() {}
    }

    /**
     * Prompt type constants.
     */
    public static final class PromptType {
        public static final String PREDEFINED = "PREDEFINED";
        public static final String CUSTOM = "CUSTOM";

        private PromptType() {}
    }

    /**
     * Predefined prompt keys.
     */
    public static final class PromptKey {
        public static final String DAY_ANALYSIS = "day_analysis";
        public static final String SWING_TRADE = "swing_trade";
        public static final String RISK_ASSESSMENT = "risk_assessment";
        public static final String MARKET_STRUCTURE = "market_structure";
        public static final String ENTRY_EXIT = "entry_exit";
        public static final String GENERAL_ANALYZER = "general_analyzer";

        private PromptKey() {}
    }

    /**
     * Alert type constants.
     */
    public static final class AlertType {
        public static final String CALL_SMS_TELEGRAM = "CALL_SMS_TELEGRAM";
        public static final String SMS_TELEGRAM = "SMS_TELEGRAM";
        public static final String LOG_ONLY = "LOG_ONLY";

        private AlertType() {}
    }

    /**
     * HTTP and API constants.
     */
    public static final class Api {
        public static final String SUCCESS_STATUS = "success";
        public static final String ERROR_STATUS = "error";
        public static final String WARNING_STATUS = "warning";
        
        private Api() {}
    }

    /**
     * Time zone constants.
     */
    public static final class TimeZones {
        public static final String NEW_YORK = "America/New_York";
        public static final String UTC = "UTC";
        
        private TimeZones() {}
    }

    /**
     * Default configuration values.
     */
    public static final class Defaults {
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        public static final int DEFAULT_TIMEOUT_SECONDS = 60;
        public static final int DEFAULT_MAX_RETRIES = 3;
        
        private Defaults() {}
    }
}
