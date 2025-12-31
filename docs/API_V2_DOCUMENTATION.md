# AI Trade Finder API Documentation v2.0
## Dynamic, Configurable, AI-Powered Trading System

## Overview

Version 2.0 introduces major architectural improvements:
- **Dynamic Scheduler Configuration** - Create/manage schedulers via API
- **AI Chat Sessions** - Multi-turn conversations about trades
- **OpenAI Structured Outputs** - Type-safe AI responses
- **Unified AI Controller** - Single endpoint for all AI operations
- **Real-time Configuration** - No deployments needed for changes

---

## Table of Contents

1. [API V2 - Unified AI Controller](#api-v2---unified-ai-controller)
2. [Dynamic Scheduler Management](#dynamic-scheduler-management)
3. [Trade Finder API](#trade-finder-api)
4. [Chat Sessions](#chat-sessions)
5. [Configuration Management](#configuration-management)

---

## API V2 - Unified AI Controller

Base Path: `/api/v2/ai`

### 1. Structured Trade Analysis

```http
POST /api/v2/ai/analyze/trade
Content-Type: application/json

{
  "symbol": "NQ",
  "timeframe": "5m",
  "marketData": {
    "price": 21850,
    "volume": 150000
  },
  "indicators": ["RSI", "MACD", "CISD"]
}
```

**Response**: TradeSignalResponseDTO (structured output)
```json
{
  "status": "TRADE_IDENTIFIED",
  "direction": "LONG",
  "symbol": "NQ",
  "timeframe": "5m",
  "confidence": 85,
  "entry": {
    "zone_type": "FVG_CE",
    "zone": "21780-21800",
    "price": 21790.0
  },
  "stop": {
    "placement": "Below 21750",
    "price": 21750.0
  },
  "targets": [
    {"level": "POC", "price": 21850.0}
  ]
}
```

### 2. Generic Structured Analysis

```http
POST /api/v2/ai/analyze
Content-Type: application/json

{
  "analysisType": "MARKET_STRUCTURE",
  "input": "Analyze NQ price action from last 2 hours",
  "systemInstructions": "You are a market structure expert",
  "maxTokens": 4000,
  "temperature": 0.7,
  "responseFormat": "json_object"
}
```

---

## Chat Sessions

### Create Chat Session

```http
POST /api/v2/ai/chat/sessions
Content-Type: application/json

{
  "title": "NQ Long Setup Discussion",
  "type": "TRADE_DISCUSSION",
  "tradeId": "65a1b2c3d4e5f6g7h8i9j0k1",
  "symbol": "NQ"
}
```

**Response**:
```json
{
  "id": "chat_abc123",
  "title": "NQ Long Setup Discussion",
  "type": "TRADE_DISCUSSION",
  "tradeId": "65a1b2c3d4e5f6g7h8i9j0k1",
  "symbol": "NQ",
  "active": true,
  "messages": [],
  "createdAt": "2025-12-31T10:00:00Z"
}
```

### Send Message in Chat

```http
POST /api/v2/ai/chat/sessions/{sessionId}/messages
Content-Type: application/json

{
  "message": "Why is this a high-confidence setup?"
}
```

**Response**:
```json
{
  "sessionId": "chat_abc123",
  "message": "This is high-confidence because it combines 3 key elements: liquidity sweep at 21750, strong bullish CISD on 5m, and FVG entry zone at 21780-21800. The confluence of these Smart Money signals suggests institutional positioning for upside...",
  "timestamp": "2025-12-31T10:01:00Z",
  "model": "gpt-4o"
}
```

### Discuss Specific Trade

```http
POST /api/v2/ai/chat/discuss-trade
Content-Type: application/json

{
  "tradeId": "65a1b2c3d4e5f6g7h8i9j0k1",
  "initialQuestion": "What are the risks with this setup?"
}
```

Creates a chat session pre-loaded with trade context and sends your initial question.

### Get Chat Sessions for Trade

```http
GET /api/v2/ai/chat/trades/{tradeId}/sessions
```

### Get Active Chat Sessions

```http
GET /api/v2/ai/chat/sessions/active
```

### Close Chat Session

```http
POST /api/v2/ai/chat/sessions/{sessionId}/close
```

---

## Dynamic Scheduler Management

Base Path: `/api/v2/schedulers`

### Create New Scheduler

```http
POST /api/v2/schedulers
Content-Type: application/json

{
  "name": "custom-analysis-es",
  "description": "Custom ES analysis every 10 minutes",
  "type": "CUSTOM",
  "enabled": true,
  "scheduleExpression": "600000",
  "scheduleType": "FIXED_RATE",
  "parameters": {
    "symbol": "ES",
    "timeframes": ["5m", "15m"],
    "minConfidence": 70
  },
  "handlerBean": "aiTradeFinderService",
  "handlerMethod": "findTrades",
  "priority": 5
}
```

**Response**: Created scheduler configuration with ID

### Schedule Types

1. **CRON**: Use cron expression
   ```json
   {
     "scheduleType": "CRON",
     "scheduleExpression": "0 */5 * * * *"
   }
   ```

2. **FIXED_RATE**: Fixed interval in milliseconds
   ```json
   {
     "scheduleType": "FIXED_RATE",
     "scheduleExpression": "300000"
   }
   ```

3. **FIXED_DELAY**: Delay after completion
   ```json
   {
     "scheduleType": "FIXED_DELAY",
     "scheduleExpression": "60000"
   }
   ```

### Get All Schedulers

```http
GET /api/v2/schedulers
```

### Get Scheduler by Name

```http
GET /api/v2/schedulers/{name}
```

### Update Scheduler

```http
PUT /api/v2/schedulers/{name}
Content-Type: application/json

{
  "description": "Updated description",
  "enabled": true,
  "scheduleExpression": "120000",
  "scheduleType": "FIXED_RATE",
  "parameters": {...}
}
```

### Enable/Disable Scheduler

```http
POST /api/v2/schedulers/{name}/enable
POST /api/v2/schedulers/{name}/disable
```

### Delete Scheduler

```http
DELETE /api/v2/schedulers/{name}
```

### Get Scheduler Statistics

```http
GET /api/v2/schedulers/statistics
```

**Response**:
```json
{
  "totalSchedulers": 5,
  "enabledSchedulers": 4,
  "activeSchedulers": 4,
  "totalExecutions": 1247,
  "totalFailures": 3,
  "schedulerNames": {
    "trade-finder-nq": true,
    "trade-finder-es": true,
    "expiration-checker": true,
    "statistics-logger": true
  }
}
```

### Get Active Schedulers

```http
GET /api/v2/schedulers/active
```

---

## Trade Finder API (V1 - Still Supported)

Base Path: `/api/v1/trade-finder`

### Manual Trigger

```http
POST /api/v1/trade-finder/trigger
```

### Get Trades by Symbol

```http
GET /api/v1/trade-finder/trades/{symbol}
```

### Get Recent Trades

```http
GET /api/v1/trade-finder/trades/recent?hours=24
```

### Get Statistics

```http
GET /api/v1/trade-finder/statistics
```

### Health Check

```http
GET /api/v1/trade-finder/health
```

---

## Example Workflows

### Workflow 1: Create Custom Scheduler for Specific Symbol

```bash
# 1. Create scheduler
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ym-finder",
    "description": "YM-specific trade finder",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "180000",
    "scheduleType": "FIXED_RATE",
    "parameters": {
      "symbols": ["YM"],
      "minConfidence": 75
    },
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 10
  }'

# 2. Check it's running
curl http://localhost:8082/api/v2/schedulers/active

# 3. View results
curl http://localhost:8082/api/v1/trade-finder/trades/YM
```

### Workflow 2: Discuss a Trade with AI

```bash
# 1. Find a trade
TRADE_ID=$(curl http://localhost:8082/api/v1/trade-finder/trades/NQ | jq -r '.[0].id')

# 2. Start discussion
curl -X POST http://localhost:8082/api/v2/ai/chat/discuss-trade \
  -H "Content-Type: application/json" \
  -d "{
    \"tradeId\": \"$TRADE_ID\",
    \"initialQuestion\": \"What's the risk-reward on this setup?\"
  }"

# 3. Continue conversation
SESSION_ID=$(curl http://localhost:8082/api/v2/ai/chat/trades/$TRADE_ID/sessions | jq -r '.[0].id')

curl -X POST http://localhost:8082/api/v2/ai/chat/sessions/$SESSION_ID/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "Should I wait for confirmation or enter at current price?"}'
```

### Workflow 3: Dynamic Scheduler Modification

```bash
# Disable default scheduler during volatile events
curl -X POST http://localhost:8082/api/v2/schedulers/trade-finder-main/disable

# Create temporary high-frequency checker
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "fomc-monitor",
    "description": "High-frequency monitoring during FOMC",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "60000",
    "scheduleType": "FIXED_RATE",
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 100
  }'

# Later, delete temporary scheduler
curl -X DELETE http://localhost:8082/api/v2/schedulers/fomc-monitor

# Re-enable default
curl -X POST http://localhost:8082/api/v2/schedulers/trade-finder-main/enable
```

---

## WebSocket Support (Future)

Coming soon: Real-time trade alerts and chat via WebSocket

```javascript
// Future WebSocket endpoint
const ws = new WebSocket('ws://localhost:8082/ws/trades');
ws.onmessage = (event) => {
  const trade = JSON.parse(event.data);
  console.log('New trade:', trade);
};
```

---

## Rate Limits & Best Practices

1. **API Rate Limits**: 100 requests/minute per endpoint
2. **Chat Sessions**: Max 50 messages per session
3. **Schedulers**: Max 20 concurrent schedulers
4. **Minimum Interval**: 30 seconds for schedulers

---

## Error Handling

All endpoints return standard HTTP status codes:

- `200 OK`: Success
- `201 Created`: Resource created
- `400 Bad Request`: Invalid input
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource already exists
- `500 Internal Server Error`: Server error

Error response format:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Scheduler name already exists",
  "timestamp": "2025-12-31T10:00:00Z",
  "path": "/api/v2/schedulers"
}
```

---

## Authentication (Future)

Currently open APIs. Future versions will include:
- JWT authentication
- API key management
- Role-based access control (RBAC)

---

## Monitoring Endpoints

```http
GET /actuator/health
GET /actuator/metrics
GET /actuator/scheduledtasks
```

---

**Version**: 2.0  
**Last Updated**: 2025-12-31  
**Author**: AI Trade Finder Team
