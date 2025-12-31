# AI Workflow API - Enhanced Multi-DataSource Architecture

## Overview
The refactored AI Workflow API uses a **Strategy Pattern** with pluggable data sources, allowing you to analyze market data from multiple sources simultaneously. The system is designed for extensibility - new data sources can be added without modifying existing code.

---

## Architecture Highlights

### Data Source Strategy Pattern
```
DataSource (Interface)
    ├── CoreMarketEventDataSource (Events from indicators)
    ├── OHLCDataSource (Candlestick data)
    ├── VolumeProfileDataSource (Future implementation)
    └── OrderBookDataSource (Future implementation)
```

### Key Components
- **DataSource Interface**: Generic contract for all data sources
- **DataSourceFactory**: Auto-discovers and manages data source implementations
- **DataSourceConfig**: Per-source configuration (time range, filters, limits)
- **TimeFrameConfig**: Container for multiple data source configurations per timeframe

---

## New Endpoints

### 1. List Available Data Sources
**GET** `/api/v1/ai/data-sources`

Returns all registered data source types with descriptions.

**Response:**
```json
{
  "dataSources": {
    "core_market_event": "Core market events from technical indicators including RSI, MACD, EMA crossovers, and custom signals",
    "ohlc": "OHLC candlestick data with open, high, low, close prices and volume at various timeframes"
  },
  "count": 2
}
```

---

### 2. Data Source Health Check
**GET** `/api/v1/ai/data-sources/health`

Checks connectivity and health of all data sources.

**Response:**
```json
{
  "healthStatus": {
    "core_market_event": true,
    "ohlc": true
  },
  "overallStatus": "HEALTHY",
  "timestamp": 1703775123456
}
```

---

## Updated Workflow Request Format

### Multi-DataSource Example
**POST** `/api/v1/ai/workflow`

```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "day_analysis",
  "symbol": "AAPL",
  "additionalContext": "Looking for scalping opportunities",
  "timeframeSettings": {
    "5m": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "CORE_MARKET_EVENT",
          "enabled": true,
          "fromTime": "2024-01-15T09:30:00Z",
          "toTime": "2024-01-15T16:00:00Z",
          "maxRecords": 50,
          "filterCriteria": "indicator:RSI|MACD"
        },
        {
          "dataSourceType": "OHLC",
          "enabled": true,
          "fromTime": "2024-01-15T09:00:00Z",
          "toTime": "2024-01-15T16:30:00Z",
          "maxRecords": 100,
          "useExternalSource": false
        }
      ]
    },
    "1h": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "OHLC",
          "enabled": true,
          "fromTime": "2024-01-10T00:00:00Z",
          "toTime": "2024-01-15T23:59:59Z",
          "useExternalSource": true
        }
      ]
    }
  },
  "dryRun": false
}
```

---

## Request Structure

### AIWorkflowRequest Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `promptType` | String | Yes | "PREDEFINED" or "CUSTOM" |
| `selectedPredefinedPrompt` | String | Conditional | Required if promptType is "PREDEFINED" |
| `customPromptText` | String | Conditional | Required if promptType is "CUSTOM" |
| `symbol` | String | Yes | Trading symbol (e.g., "AAPL", "NQ") |
| `additionalContext` | String | No | Extra context for the AI |
| `timeframeSettings` | Object | Yes | Map of timeframes to configurations |
| `dryRun` | Boolean | No | If true, returns prompt without AI call |
| `manualDataset` | Array | No | Additional data to include |

### TimeFrameConfig Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `enabled` | Boolean | No | Whether this timeframe is enabled (default: true) |
| `dataSources` | Array | Yes | List of data source configurations |

### DataSourceConfig Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `dataSourceType` | Enum | Yes | "CORE_MARKET_EVENT", "OHLC", etc. |
| `enabled` | Boolean | No | Whether this data source is enabled (default: true) |
| `fromTime` | ISO 8601 | Yes | Start timestamp for data retrieval |
| `toTime` | ISO 8601 | Yes | End timestamp for data retrieval |
| `maxRecords` | Integer | No | Limit number of records (0 or null = no limit) |
| `useExternalSource` | Boolean | No | Use external data provider (for OHLC) |
| `filterCriteria` | String | No | Source-specific filters |

---

## Filter Criteria Examples

### CoreMarketEvent Filters
```
"filterCriteria": "indicator:RSI|MACD"     // Only RSI and MACD indicators
"filterCriteria": "queued:false"           // Only unqueued events
"filterCriteria": "indicator:RSI,queued:false"  // Combined filters
```

### OHLC Filters
Currently, OHLC data source uses `useExternalSource` flag instead of filterCriteria.

---

## Usage Examples

### Example 1: Day Trading with Multiple Data Sources
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "day_analysis",
  "symbol": "TSLA",
  "timeframeSettings": {
    "1m": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "CORE_MARKET_EVENT",
          "enabled": true,
          "fromTime": "2024-01-15T09:30:00Z",
          "toTime": "2024-01-15T11:00:00Z",
          "maxRecords": 20
        },
        {
          "dataSourceType": "OHLC",
          "enabled": true,
          "fromTime": "2024-01-15T09:30:00Z",
          "toTime": "2024-01-15T11:00:00Z",
          "maxRecords": 90
        }
      ]
    },
    "5m": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "OHLC",
          "enabled": true,
          "fromTime": "2024-01-15T09:00:00Z",
          "toTime": "2024-01-15T11:30:00Z"
        }
      ]
    }
  },
  "dryRun": false
}
```

### Example 2: Swing Trading with OHLC Only
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "swing_trade",
  "symbol": "NVDA",
  "additionalContext": "Looking for 3-5 day hold",
  "timeframeSettings": {
    "1h": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "OHLC",
          "enabled": true,
          "fromTime": "2024-01-08T00:00:00Z",
          "toTime": "2024-01-15T23:59:59Z",
          "useExternalSource": true
        }
      ]
    },
    "1d": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "OHLC",
          "enabled": true,
          "fromTime": "2023-12-01T00:00:00Z",
          "toTime": "2024-01-15T23:59:59Z",
          "maxRecords": 30
        }
      ]
    }
  },
  "dryRun": false
}
```

### Example 3: Events Only Analysis
```json
{
  "promptType": "CUSTOM",
  "customPromptText": "Analyze indicator signals for momentum trading setups",
  "symbol": "SPY",
  "timeframeSettings": {
    "5m": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "CORE_MARKET_EVENT",
          "enabled": true,
          "fromTime": "2024-01-15T09:30:00Z",
          "toTime": "2024-01-15T16:00:00Z",
          "filterCriteria": "indicator:RSI|MACD|EMA"
        }
      ]
    }
  },
  "dryRun": false
}
```

### Example 4: Dry Run to Test Prompt
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "market_structure",
  "symbol": "NQ",
  "timeframeSettings": {
    "15m": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "OHLC",
          "enabled": true,
          "fromTime": "2024-01-15T09:00:00Z",
          "toTime": "2024-01-15T17:00:00Z"
        },
        {
          "dataSourceType": "CORE_MARKET_EVENT",
          "enabled": true,
          "fromTime": "2024-01-15T09:00:00Z",
          "toTime": "2024-01-15T17:00:00Z",
          "maxRecords": 30
        }
      ]
    }
  },
  "dryRun": true
}
```

---

## Response Format

### Success Response
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "completed",
  "output": "**DAY TRADING ANALYSIS - AAPL**\n\n...(AI analysis)...",
  "model": "gpt-4",
  "usage": {
    "promptTokens": 2340,
    "completionTokens": 890,
    "totalTokens": 3230
  }
}
```

### Dry Run Response
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "dry_run",
  "output": "### SYSTEM INSTRUCTIONS ###\n...(full generated prompt)...",
  "model": "N/A"
}
```

---

## Adding New Data Sources

To add a new data source (e.g., Volume Profile):

1. **Create Data Source Implementation**
```java
@Component
public class VolumeProfileDataSource implements DataSource {
    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.VOLUME_PROFILE;
    }
    
    @Override
    public DataSourceResult fetchData(String symbol, String timeframe, DataSourceConfig config) {
        // Implementation
    }
    // ... other methods
}
```

2. **Add to DataSourceType Enum**
```java
VOLUME_PROFILE("volume_profile", "Volume Profile Data")
```

3. **Done!** The factory automatically discovers and registers it.

---

## Benefits of This Architecture

✅ **Extensibility**: Add new data sources without touching existing code  
✅ **Flexibility**: Users choose which data sources they need  
✅ **Performance**: Fetch only required data with configurable limits  
✅ **Testability**: Easy to mock data sources for testing  
✅ **Maintainability**: Clear separation of concerns  
✅ **Type Safety**: Compile-time checks for data source types  
✅ **Health Monitoring**: Built-in health checks for all data sources  
✅ **Filtering**: Source-specific filtering capabilities  

---

## Error Handling

### Invalid Data Source Type
```json
{
  "status": "failed",
  "output": "Workflow execution failed: No data source implementation registered for type: INVALID_TYPE"
}
```

### Data Source Unavailable
Individual data source errors don't fail the entire request. The prompt includes error messages:
```
### MARKET DATA FOR ANALYSIS (AAPL) ###

--- TIMEFRAME: 5m ---

Core Market Events:
Error: Database connection timeout

OHLC Data:
(successful data retrieval)
...
```

---

## Migration Guide

### Old Format (Legacy)
```json
{
  "timeframeSettings": {
    "5m": {
      "includeEvents": true,
      "eventsFrom": "2024-01-15T09:30:00Z",
      "eventsTo": "2024-01-15T16:00:00Z",
      "maxEvents": 50
    }
  }
}
```

### New Format
```json
{
  "timeframeSettings": {
    "5m": {
      "enabled": true,
      "dataSources": [
        {
          "dataSourceType": "CORE_MARKET_EVENT",
          "enabled": true,
          "fromTime": "2024-01-15T09:30:00Z",
          "toTime": "2024-01-15T16:00:00Z",
          "maxRecords": 50
        }
      ]
    }
  }
}
```
