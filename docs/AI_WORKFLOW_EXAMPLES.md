# AI Workflow API Examples

## Overview
The AI Workflow API allows you to analyze market data using predefined trading strategies or custom prompts, with support for multi-timeframe analysis.

---

## Endpoints

### 1. Get Available Prompts
**GET** `/api/v1/ai/prompts`

Returns a list of available predefined prompt templates.

**Response:**
```json
{
  "day_analysis": "Day Trading Analysis",
  "swing_trade": "Swing Trading Strategy",
  "risk_assessment": "Risk Assessment & Position Sizing",
  "market_structure": "Market Structure Analysis",
  "entry_exit": "Entry & Exit Planning",
  "general_analyzer": "General Market Analyzer"
}
```

---

### 2. Execute AI Workflow (Dry Run)
**POST** `/api/v1/ai/workflow`

Test your request configuration by getting the generated prompt without calling the AI service.

**Request:**
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "day_analysis",
  "symbol": "AAPL",
  "additionalContext": "Focus on scalping opportunities in the first hour",
  "timeframeSettings": {
    "5m": {
      "includeEvents": true,
      "eventsFrom": "2024-01-15T09:30:00Z",
      "eventsTo": "2024-01-15T10:30:00Z",
      "maxEvents": 20
    }
  },
  "dryRun": true
}
```

**Response:**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "dry_run",
  "output": "### SYSTEM INSTRUCTIONS ###\nYou are an expert day trading analyst...\n\n### USER PROVIDED CONTEXT ###\nFocus on scalping opportunities in the first hour\n\n### MARKET DATA FOR ANALYSIS (AAPL) ###\n...",
  "model": "N/A"
}
```

---

### 3. Execute AI Workflow (Live Analysis)
**POST** `/api/v1/ai/workflow`

Execute the workflow with AI analysis.

**Request:**
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "swing_trade",
  "symbol": "TSLA",
  "additionalContext": "Looking for 3-5 day hold opportunities",
  "timeframeSettings": {
    "1h": {
      "includeEvents": true,
      "eventsFrom": "2024-01-10T00:00:00Z",
      "eventsTo": "2024-01-15T23:59:59Z",
      "maxEvents": 100
    },
    "4h": {
      "includeEvents": true,
      "eventsFrom": "2024-01-01T00:00:00Z",
      "eventsTo": "2024-01-15T23:59:59Z",
      "maxEvents": 50
    }
  },
  "dryRun": false
}
```

**Response:**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "completed",
  "output": "**SWING TRADING ANALYSIS - TSLA**\n\nTrading Bias: BULLISH (High Confidence)\n\nEntry Zone: $245.50 - $248.00\nStop Loss: $240.00\n\nProfit Targets:\n- T1: $255.00 (30% position)\n- T2: $262.00 (40% position)\n- T3: $270.00+ (30% position)\n\nExpected Holding Period: 3-5 days\n\nKey Observations:\n- Multiple bullish indicators converging on 1H timeframe\n- 4H timeframe showing higher lows formation\n- Volume increasing on up moves...",
  "model": "gpt-4",
  "usage": {
    "promptTokens": 1234,
    "completionTokens": 567,
    "totalTokens": 1801
  }
}
```

---

### 4. Custom Prompt Analysis
**POST** `/api/v1/ai/workflow`

Use a custom prompt for specialized analysis.

**Request:**
```json
{
  "promptType": "CUSTOM",
  "customPromptText": "You are a momentum trading specialist. Analyze the market data and identify explosive breakout setups with clear stop-loss levels. Focus on high-probability setups only.",
  "symbol": "NVDA",
  "additionalContext": "Looking for gap-and-go patterns after earnings",
  "timeframeSettings": {
    "5m": {
      "includeEvents": true,
      "eventsFrom": "2024-01-15T09:30:00Z",
      "eventsTo": "2024-01-15T16:00:00Z",
      "maxEvents": 50
    },
    "15m": {
      "includeEvents": true,
      "eventsFrom": "2024-01-15T09:00:00Z",
      "eventsTo": "2024-01-15T16:00:00Z",
      "maxEvents": 30
    }
  },
  "dryRun": false
}
```

---

### 5. Multi-Timeframe Analysis with Manual Data
**POST** `/api/v1/ai/workflow`

Include manual data points along with market events.

**Request:**
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "risk_assessment",
  "symbol": "SPY",
  "additionalContext": "Portfolio allocation review for conservative account",
  "timeframeSettings": {
    "1d": {
      "includeEvents": true,
      "eventsFrom": "2024-01-01T00:00:00Z",
      "eventsTo": "2024-01-15T23:59:59Z"
    }
  },
  "manualDataset": [
    {
      "note": "Major support zone",
      "level": 475.50,
      "significance": "High"
    },
    {
      "note": "Previous swing high resistance",
      "level": 482.75,
      "significance": "Medium"
    }
  ],
  "dryRun": false
}
```

---

## Request Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `promptType` | String | Yes | "PREDEFINED" or "CUSTOM" |
| `selectedPredefinedPrompt` | String | If PREDEFINED | Key of predefined prompt |
| `customPromptText` | String | If CUSTOM | Your custom analysis instructions |
| `symbol` | String | Yes | Trading symbol (e.g., "AAPL") |
| `additionalContext` | String | No | Extra context for the AI |
| `timeframeSettings` | Object | Yes | Map of timeframes to configurations |
| `dryRun` | Boolean | No | If true, returns prompt without AI call |
| `manualDataset` | Array | No | Additional data to include |

### TimeFrameConfig Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `includeEvents` | Boolean | Yes | Whether to include event data |
| `eventsFrom` | ISO 8601 | If includeEvents | Start timestamp for events |
| `eventsTo` | ISO 8601 | If includeEvents | End timestamp for events |
| `maxEvents` | Integer | No | Limit number of events |

---

## Common Use Cases

### Day Trading Setup
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "day_analysis",
  "symbol": "AAPL",
  "timeframeSettings": {
    "1m": {
      "includeEvents": true,
      "eventsFrom": "2024-01-15T09:30:00Z",
      "eventsTo": "2024-01-15T11:00:00Z",
      "maxEvents": 30
    },
    "5m": {
      "includeEvents": true,
      "eventsFrom": "2024-01-15T09:00:00Z",
      "eventsTo": "2024-01-15T11:00:00Z"
    }
  },
  "dryRun": false
}
```

### Swing Trading Setup
```json
{
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "swing_trade",
  "symbol": "TSLA",
  "timeframeSettings": {
    "1h": {
      "includeEvents": true,
      "eventsFrom": "2024-01-08T00:00:00Z",
      "eventsTo": "2024-01-15T23:59:59Z"
    },
    "1d": {
      "includeEvents": true,
      "eventsFrom": "2023-12-01T00:00:00Z",
      "eventsTo": "2024-01-15T23:59:59Z"
    }
  },
  "dryRun": false
}
```

---

## Error Responses

### Missing Symbol
```json
{
  "status": "error",
  "output": "Symbol is required"
}
```

### Missing Prompt Type
```json
{
  "status": "error",
  "output": "Prompt type is required (PREDEFINED or CUSTOM)"
}
```

### AI Service Failure
```json
{
  "status": "failed",
  "output": "Workflow execution failed: Connection timeout"
}
```
