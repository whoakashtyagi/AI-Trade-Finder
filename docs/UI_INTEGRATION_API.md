# UI Integration API Reference (AI Trade Finder)

**Last updated**: 2025-12-31

This document is written for UI/front-end integration. It enumerates every REST API currently implemented in the project (based on Spring controllers + DTOs/documents) and includes:
- Endpoint list (method + path)
- Request/response shapes (JSON)
- Field types and constraints (where enforced or explicitly defined)
- Known allowed values (constants/enums from code)
- Common error/status behaviors

> Notes on "all cases possible": Some parts of this system intentionally accept free-form data (e.g., `marketData: Map<String,Object>`, `meta: Map<String,Object>`, generic structured AI JSON). In these areas, the backend does not strictly validate shape; the UI should treat them as flexible objects and display/submit them accordingly.

---

## 1) Runtime + Base URL

- **Default server port**: `8082` (see `server.port` in `application.properties`)
- **Local base URL**: `http://localhost:8082`
- **Content type**: `application/json`
- **Authentication**: none implemented (all endpoints are open)

---

## 2) Common Response Shapes

### 2.1 `ApiResponse<T>` (used by some v1 endpoints)
Used by:
- `POST /api/v1/trade-finder/trigger`
- `GET /api/v1/trade-finder/statistics`
- `GET /api/v1/trade-finder/health`

Shape:
```json
{
  "status": "success",
  "message": "Optional human-readable message",
  "timestamp": "2025-12-31T10:00:00Z",
  "data": {},
  "metadata": {}
}
```

- `status`: string. In code the success value is `"success"`.
- `timestamp`: ISO-8601 string (serialized `Instant`).
- `data`: depends on endpoint.
- `metadata`: optional; may be null.

### 2.2 `ErrorResponse` (global exception handler)
When exceptions are thrown (and not swallowed by controllers), the application may return a consistent error object.

Shape:
```json
{
  "timestamp": "2025-12-31T10:00:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "What went wrong",
  "path": "/api/v2/ai/analyze",
  "requestId": "Optional",
  "details": {
    "retryable": false,
    "statusCode": 502,
    "errorCode": "Optional"
  }
}
```

Important UI note:
- Several controllers **catch `Exception` and return an empty body** with HTTP `500` (or sometimes `400`). In those specific endpoints/cases you may **not** receive `ErrorResponse`.

---

## 3) Global Allowed Values (Constants)

These values come from `com.trade.app.util.Constants` and model/document comments.

### 3.1 Trade status (`IdentifiedTrade.status`)
String. Known values:
- `IDENTIFIED`
- `ALERTED`
- `EXPIRED`
- `CANCELLED`
- `TAKEN`
- `INVALIDATED`

### 3.2 Trade direction (`IdentifiedTrade.direction`, trade signal direction)
String. Known values:
- `LONG`
- `SHORT`

### 3.3 Trade signal analysis status (`TradeSignalResponseDTO.status`)
String. Known values:
- `TRADE_IDENTIFIED`
- `NO_SETUP`
- `INSUFFICIENT_DATA`
- `ERROR`

### 3.4 Entry zone type (trade signal + identified trade)
String. Known values:
- `FVG_CE`
- `IFVG`
- `OB`
- `BREAKER`
- `MITIGATION`

### 3.5 Alert type (`IdentifiedTrade.alertType`)
String. Known values:
- `CALL_SMS_TELEGRAM`
- `SMS_TELEGRAM`
- `LOG_ONLY`

### 3.6 Conversation status (`AIConversation.status`)
String. Known values:
- `ACTIVE`
- `COMPLETED`
- `EXPIRED`

### 3.7 Conversation type (`AIConversation.conversationType`)
String. Known values:
- `TRADE_FOLLOWUP`
- `TRADE_ANALYSIS`
- `MARKET_ANALYSIS`
- `WORKFLOW`

### 3.8 Conversation entity type (`AIConversation.entityType`)
String. Known values:
- `TRADE`
- `SYMBOL`
- `PATTERN`

Model/doc comments also mention other possible values like `MARKET`, `GENERAL` — those are not declared in `Constants` but are accepted as plain strings.

### 3.9 Chat session type (`ChatSession.type`)
String. Controller docs suggest:
- `TRADE_DISCUSSION`
- `MARKET_ANALYSIS`
- `GENERAL`

(Backend stores this as a plain string and does not enforce an enum.)

### 3.10 Scheduler schedule type (`SchedulerConfig.scheduleType`)
String. Known values by comment:
- `CRON`
- `FIXED_RATE`
- `FIXED_DELAY`

### 3.11 Scheduler type (`SchedulerConfig.type`)
String. Known values by comment:
- `TRADE_FINDER`
- `EXPIRATION`
- `STATISTICS`
- `CUSTOM`

---

## 4) API Surface Map

### 4.1 API v2 (AI)
Base path: `/api/v2/ai`

- `POST /api/v2/ai/analyze/trade`
- `POST /api/v2/ai/analyze`
- `POST /api/v2/ai/chat/sessions`
- `POST /api/v2/ai/chat/sessions/{sessionId}/messages`
- `GET  /api/v2/ai/chat/sessions/{sessionId}`
- `GET  /api/v2/ai/chat/trades/{tradeId}/sessions`
- `POST /api/v2/ai/chat/sessions/{sessionId}/close`
- `GET  /api/v2/ai/chat/sessions/active`
- `POST /api/v2/ai/chat/discuss-trade`

### 4.2 API v2 (Schedulers)
Base path: `/api/v2/schedulers`

- `POST   /api/v2/schedulers`
- `GET    /api/v2/schedulers`
- `GET    /api/v2/schedulers/{name}`
- `PUT    /api/v2/schedulers/{name}`
- `DELETE /api/v2/schedulers/{name}`
- `POST   /api/v2/schedulers/{name}/enable`
- `POST   /api/v2/schedulers/{name}/disable`
- `GET    /api/v2/schedulers/statistics`
- `GET    /api/v2/schedulers/active`

### 4.3 API v2 (Operations / UI Integration)
Base paths: `/api/v2/operations`, `/api/v2/ui`

- `GET /api/v2/operations/logs`
- `GET /api/v2/operations/{operationId}`
- `GET /api/v2/operations/logs/{id}`
- `GET /api/v2/ui/constants`

### 4.4 API v1 (Trade Finder)
Base path: `/api/v1/trade-finder`

- `POST /api/v1/trade-finder/trigger`
- `GET  /api/v1/trade-finder/trades/{symbol}`
- `GET  /api/v1/trade-finder/trades/{symbol}/status/{status}`
- `GET  /api/v1/trade-finder/trades/recent?hours=24`
- `GET  /api/v1/trade-finder/trades?symbol=&status=&direction=&minConfidence=&hours=24&limit=200` *(UI-friendly list)*
- `GET  /api/v1/trade-finder/trades/id/{tradeId}` *(get by id)*
- `PATCH /api/v1/trade-finder/trades/id/{tradeId}/status` *(update status)*
- `GET  /api/v1/trade-finder/summary?hours=24` *(dashboard summary)*
- `GET  /api/v1/trade-finder/statistics?hours=24`
- `GET  /api/v1/trade-finder/health`

### 4.5 API v1 (Market Events)
Base path: `/api/v1/market-events`

- `POST   /api/v1/market-events`
- `GET    /api/v1/market-events`
- `GET    /api/v1/market-events/{id}`
- `GET    /api/v1/market-events/symbol/{symbol}?start=...&end=...`
- `GET    /api/v1/market-events/symbol/{symbol}/timeframe/{timeframe}?start=...&end=...`
- `PUT    /api/v1/market-events/{id}`
- `DELETE /api/v1/market-events/{id}`
- `GET    /api/v1/market-events/health`

### 4.6 API v1 (AI Conversation State)
Base path: `/api/v1/conversations`

- `POST /api/v1/conversations`
- `GET  /api/v1/conversations/{conversationId}`
- `GET  /api/v1/conversations/{conversationId}/stats`
- `GET  /api/v1/conversations/symbol/{symbol}`
- `POST /api/v1/conversations/{conversationId}/complete`
- `POST /api/v1/conversations/cleanup`

### 4.7 API v1 (OHLC Data)
Base path: `/api/v1/ohlc`

- `POST   /api/v1/ohlc`
- `GET    /api/v1/ohlc`
- `GET    /api/v1/ohlc/{id}`
- `GET    /api/v1/ohlc/symbol/{symbol}/timeframe/{timeframe}?start=...&end=...`
- `GET    /api/v1/ohlc/symbol/{symbol}/timeframe/{timeframe}/epoch?startMillis=...&endMillis=...`
- `GET    /api/v1/ohlc/symbol/{symbol}/timeframe/{timeframe}/latest`
- `PUT    /api/v1/ohlc/{id}`
- `DELETE /api/v1/ohlc/{id}`
- `GET    /api/v1/ohlc/health`

### 4.8 API v1 (Transformed Events)
Base path: `/api/v1/transformed-events`

- `POST   /api/v1/transformed-events`
- `GET    /api/v1/transformed-events`
- `GET    /api/v1/transformed-events/{id}`
- `GET    /api/v1/transformed-events/symbol/{symbol}?start=...&end=...`
- `GET    /api/v1/transformed-events/symbol/{symbol}/timeframe/{timeframe}?start=...&end=...`
- `GET    /api/v1/transformed-events/uec/{uniqueEventCode}?start=...&end=...`
- `GET    /api/v1/transformed-events/trade-signals/symbol/{symbol}/timeframe/{timeframe}?start=...&end=...`
- `PUT    /api/v1/transformed-events/{id}`
- `DELETE /api/v1/transformed-events/{id}`
- `GET    /api/v1/transformed-events/health`

---

## 5) API v2 — Unified AI Controller

### 5.1 `POST /api/v2/ai/analyze/trade`
Structured trade analysis. Returns a strongly-typed trade signal object.

**Request body: `AnalyzeTradeRequest`**
```json
{
  "symbol": "NQ",
  "timeframe": "5m",
  "marketData": {
    "price": 21850,
    "volume": 150000,
    "ohlc": [
      {"ts": "2025-12-31T09:55:00Z", "o": 21810, "h": 21855, "l": 21800, "c": 21850}
    ],
    "keyLevels": {"pdh": 21910, "pdl": 21720}
  },
  "indicators": ["RSI", "MACD", "CISD"]
}
```

Field notes:
- `symbol`: string (required by business logic; controller does not validate)
- `timeframe`: string (recommended values depend on your UI; examples: `"1m"`, `"5m"`, `"15m"`, `"1h"`)
- `marketData`: object (free-form). UI can send any nested JSON to provide context.
- `indicators`: array of strings (free-form)

**Response: `TradeSignalResponseDTO`**
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
    "price": 21790.0,
    "method": "Optional description"
  },
  "stop": {
    "placement": "Below 21750",
    "price": 21750.0,
    "reasoning": "Optional reasoning"
  },
  "targets": [
    {"level": "POC", "price": 21850.0, "description": "Optional"}
  ],
  "risk_reward": "2.5:1",
  "narrative": "Explanation text",
  "trigger_conditions": ["Condition 1", "Condition 2"],
  "invalidations": ["Invalidation 1"],
  "session_label": "NY_AM",
  "analysis_timestamp": "2025-12-31T10:00:00Z",
  "notes": "Optional"
}
```

Allowed values (UI should use dropdowns where possible):
- `status`: `TRADE_IDENTIFIED | NO_SETUP | INSUFFICIENT_DATA | ERROR`
- `direction`: `LONG | SHORT`
- `confidence`: integer $0..100$
- `entry.zone_type`: `FVG_CE | IFVG | OB | BREAKER | MITIGATION`

**Status codes**
- `200 OK`: success
- `500 Internal Server Error`: controller returns empty body (no `ErrorResponse`)

UI handling notes:
- Treat missing/empty response as failure; show a generic error.
- The server attempts to parse AI output as JSON; malformed output yields `500`.

---

### 5.2 `POST /api/v2/ai/analyze`
Generic AI analysis. Returns *arbitrary JSON* (`JsonNode`).

**Request body: `StructuredAnalysisRequest`**
```json
{
  "analysisType": "MARKET_STRUCTURE",
  "input": "Analyze NQ price action from last 2 hours",
  "systemInstructions": "You are a market structure expert",
  "maxTokens": 4000,
  "temperature": 0.7,
  "responseFormat": "json_object"
}
```

Field notes:
- `analysisType`: string (free-form label for UI)
- `input`: string (required by business logic)
- `systemInstructions`: string (optional)
- `maxTokens`: integer (optional; defaults to 4000)
- `temperature`: number (optional; defaults to 0.7)
- `responseFormat`: string (present in request DTO; current controller implementation does not actively use it)

**Response**: arbitrary JSON object
- UI must not assume a fixed schema.

**Status codes**
- `200 OK`: returns JSON
- `500 Internal Server Error`: empty body

---

## 6) API v2 — Chat Sessions

### 6.1 `POST /api/v2/ai/chat/sessions`
Creates a new chat session.

**Request body: `CreateChatSessionRequest`**
```json
{
  "title": "NQ Long Setup Discussion",
  "type": "TRADE_DISCUSSION",
  "tradeId": "65a1b2c3d4e5f6...",
  "symbol": "NQ"
}
```

Notes:
- `type` is a string; recommended values: `TRADE_DISCUSSION | MARKET_ANALYSIS | GENERAL`.
- If `tradeId` is provided and exists, the server builds trade context into `context`.

**Response: `ChatSession`** (`201 Created`)
```json
{
  "id": "chat_abc123",
  "title": "NQ Long Setup Discussion",
  "type": "TRADE_DISCUSSION",
  "tradeId": "65a1b2c3d4e5f6...",
  "symbol": "NQ",
  "messages": [],
  "context": "Trade Setup Context:\nSymbol: NQ...",
  "active": true,
  "createdAt": "2025-12-31T10:00:00Z",
  "updatedAt": null,
  "createdBy": null
}
```

**Status codes**
- `201 Created`: success
- `500 Internal Server Error`: empty body

---

### 6.2 `POST /api/v2/ai/chat/sessions/{sessionId}/messages`
Sends a message and receives an AI answer.

**Path params**
- `sessionId`: string

**Request body: `ChatMessageRequest`**
```json
{ "message": "Why is this a high-confidence setup?" }
```

**Response: `ChatMessageResponse`**
```json
{
  "sessionId": "chat_abc123",
  "message": "AI response text",
  "timestamp": "2025-12-31T10:01:00Z",
  "model": "gpt-4o"
}
```

Server-side behavior (important for UI state):
- The server appends the user message to `ChatSession.messages`.
- The server calls the AI model and appends the assistant response to `ChatSession.messages`.
- `ChatSession.updatedAt` is set.

**Status codes**
- `200 OK`: success
- `400 Bad Request`: session exists but is not active (empty body)
- `500 Internal Server Error`: any other failure (empty body)

UI handling notes:
- If you receive `400`, the session is closed; disable input and offer to create a new session.

---

### 6.3 `GET /api/v2/ai/chat/sessions/{sessionId}`
Gets full chat history and context.

**Response: `ChatSession`**
- `messages` is an array of `{ role, content, timestamp, tokens?, model? }`.
- `role` is typically `"user"` or `"assistant"`.

**Status codes**
- `200 OK`
- `404 Not Found`

---

### 6.4 `GET /api/v2/ai/chat/trades/{tradeId}/sessions`
Lists chat sessions associated with a trade.

**Response**: `ChatSession[]`

**Status codes**
- `200 OK`

---

### 6.5 `POST /api/v2/ai/chat/sessions/{sessionId}/close`
Closes a chat session.

**Response**: empty body

**Status codes**
- `200 OK`
- `500 Internal Server Error`: empty body

---

### 6.6 `GET /api/v2/ai/chat/sessions/active`
Lists active sessions.

**Response**: `ChatSession[]`

---

### 6.7 `POST /api/v2/ai/chat/discuss-trade`
Creates a trade discussion session (pre-loaded with trade context). Optionally sends an initial question.

**Request body: `DiscussTradeRequest`**
```json
{
  "tradeId": "65a1b2c3d4e5f6...",
  "initialQuestion": "What are the risks with this setup?"
}
```

**Response**: `ChatSession` (`201 Created`)

Status codes:
- `201 Created`
- `500 Internal Server Error`: empty body

UI note:
- If `initialQuestion` is provided, the server internally calls the send-message endpoint. The returned session is created even if the initial AI reply fails.

---

## 7) API v2 — Dynamic Scheduler Management

All endpoints operate on `SchedulerConfig` documents.

### 7.1 SchedulerConfig JSON shape
```json
{
  "id": "optional",
  "name": "trade-finder-nq",
  "description": "Runs trade finder for NQ",
  "type": "TRADE_FINDER",
  "enabled": true,
  "scheduleExpression": "0 */5 * * * *",
  "scheduleType": "CRON",
  "parameters": {"symbols": ["NQ"], "minConfidence": 75},
  "handlerBean": "aiTradeFinderService",
  "handlerMethod": "findTrades",
  "priority": 10,
  "lastExecution": "2025-12-31T10:00:00Z",
  "nextExecution": "2025-12-31T10:05:00Z",
  "executionCount": 12,
  "failureCount": 1,
  "lastStatus": "SUCCESS",
  "lastError": null,
  "createdAt": "2025-12-31T09:00:00Z",
  "updatedAt": "2025-12-31T10:00:00Z",
  "createdBy": null
}
```

Field notes:
- `scheduleExpression` is interpreted based on `scheduleType`.
  - For `CRON`, it should be a cron expression.
  - For `FIXED_RATE` / `FIXED_DELAY`, it should be a millisecond duration string (e.g., `"300000"`).
- `parameters` is a free-form object for the scheduled handler.

---

### 7.2 `POST /api/v2/schedulers`
Creates a new scheduler.

**Request body**: `SchedulerConfig`
- `name` must be unique.

**Status codes**
- `201 Created`: returns created SchedulerConfig
- `409 Conflict`: name already exists (empty body)
- `500 Internal Server Error`: empty body

---

### 7.3 `GET /api/v2/schedulers`
Lists all schedulers.

**Response**: `SchedulerConfig[]`

---

### 7.4 `GET /api/v2/schedulers/{name}`
Gets a scheduler by name.

**Status codes**
- `200 OK`
- `404 Not Found`

---

### 7.5 `PUT /api/v2/schedulers/{name}`
Updates an existing scheduler.

**Request body**: `SchedulerConfig`
Updatable fields (as implemented):
- `description`, `enabled`, `scheduleExpression`, `scheduleType`, `parameters`, `handlerBean`, `handlerMethod`, `priority`

**Status codes**
- `200 OK`
- `500 Internal Server Error`: includes "not found" case too (controller throws and catches, returning 500)

UI note:
- For a missing scheduler name, this endpoint currently returns `500` (not `404`). The UI should treat `500` with empty body as "not found or server error" and optionally re-fetch list.

---

### 7.6 `DELETE /api/v2/schedulers/{name}`
Deletes scheduler.

**Status codes**
- `204 No Content`
- `500 Internal Server Error`: includes "not found" case too (empty body)

---

### 7.7 `POST /api/v2/schedulers/{name}/enable`
Enables a scheduler.

**Response**: updated `SchedulerConfig`

**Status codes**
- `200 OK`
- `500 Internal Server Error` (empty body)

---

### 7.8 `POST /api/v2/schedulers/{name}/disable`
Disables a scheduler.

Same behavior as enable.

---

### 7.9 `GET /api/v2/schedulers/statistics`
Returns aggregated statistics.

**Response: `SchedulerStatistics`**
```json
{
  "totalSchedulers": 5,
  "enabledSchedulers": 4,
  "activeSchedulers": 4,
  "totalExecutions": 1247,
  "totalFailures": 3,
  "schedulerNames": {
    "trade-finder-nq": true,
    "expiration-checker": true
  }
}
```

---

### 7.10 `GET /api/v2/schedulers/active`
Returns a map of active schedulers.

```json
{
  "trade-finder-nq": true,
  "expiration-checker": true
}
```

---

## 8) API v1 — Trade Finder

### 8.1 `POST /api/v1/trade-finder/trigger`
Manually runs the trade finder process.

**Response: `ApiResponse<Map>`**
```json
{
  "status": "success",
  "message": "Trade finder execution completed",
  "timestamp": "2025-12-31T10:00:00Z",
  "data": {"duration_ms": 1234, "operationId": "5a2d1b4b-0d4a-4c2a-b7d0-1f4f0c9d6c2f"}
}
```

UI notes:
- `operationId` can be used to fetch operation logs via `GET /api/v2/operations/{operationId}` or `GET /api/v2/operations/logs?operationId=...`.

---

### 8.2 `GET /api/v1/trade-finder/trades/{symbol}`
Returns trades for a symbol.

**Response**: `IdentifiedTrade[]`

`IdentifiedTrade` shape (key UI fields):
```json
{
  "id": "...",
  "symbol": "NQ",
  "direction": "LONG",
  "identifiedAt": "2025-12-31T09:58:00Z",
  "confidence": 85,
  "status": "IDENTIFIED",

  "entryZoneType": "FVG_CE",
  "entryZone": "21780-21800",
  "entryPrice": 21790.0,

  "stopPlacement": "Below 21750",
  "targets": ["POC", "Asia High"],
  "rrHint": "2.5:1",

  "narrative": "...",
  "triggerConditions": ["..."],
  "invalidations": ["..."],
  "sessionLabel": "NY_AM",
  "timeframe": "5m",

  "dedupeKey": "NQ_LONG_21780-21800_20251231_09",
  "alertSent": false,
  "alertSentAt": null,
  "alertType": "SMS_TELEGRAM",

  "createdAt": "2025-12-31T09:58:00Z",
  "expiresAt": "2025-12-31T13:58:00Z",
  "updatedAt": null,

  "aiRequestId": "TRADE_ANALYSIS_...",
  "aiFullResponse": "...",
  "keyLevelsSnapshot": "..."
}
```

UI notes:
- `aiFullResponse` and `keyLevelsSnapshot` can be large strings; consider truncation/expand UI.

---

### 8.3 `GET /api/v1/trade-finder/trades/{symbol}/status/{status}`
Filters by status.

- `status` is a string; recommended values are in **3.1**.

---

### 8.4 `GET /api/v1/trade-finder/trades/recent?hours=24`
Returns trades across all symbols from last N hours.

Query params:
- `hours`: integer, default `24`, validated: min `1`, max `168`.

---

### 8.5 `GET /api/v1/trade-finder/statistics?hours=24`
Returns aggregated statistics.

Query params:
- `hours`: integer, default `24`, validated: min `1`, max `168`.

**Response: `ApiResponse<TradeStatisticsDTO>`**
```json
{
  "status": "success",
  "timestamp": "2025-12-31T10:00:00Z",
  "data": {
    "periodHours": 24,
    "totalTrades": 100,
    "activeTrades": 20,
    "expiredTrades": 50,
    "alertedTrades": 30,
    "averageConfidence": 71.5,
    "bySymbol": {"NQ": 40, "ES": 60},
    "byDirection": {"LONG": 55, "SHORT": 45},
    "timestamp": "2025-12-31T10:00:00Z"
  }
}
```

---

### 8.6 `GET /api/v1/trade-finder/health`
Simple health endpoint.

**Response**: `ApiResponse<Void>`

---

## 9) API v1 — Core Market Events

### 9.1 CoreMarketEvent JSON shape
```json
{
  "id": "optional",
  "symbol": "AAPL",
  "timeframe": "5m",
  "indicatorName": "RSI",
  "rawMessage": "{\"value\": 65.3, \"signal\": \"neutral\"}",
  "ingestedTs": "2025-12-31T10:00:00Z",
  "meta": {"source": "tradingview"},
  "queued": false,
  "transformAttempts": 0
}
```

Notes:
- `rawMessage` is a string; it may itself contain JSON serialized as a string.
- `meta` is a free-form object.

---

### 9.2 `POST /api/v1/market-events`
Creates a market event.

**Status codes**
- `201 Created`: returns created `CoreMarketEvent`
- `500 Internal Server Error`: empty body

---

### 9.3 `GET /api/v1/market-events`
Returns all events.

**Status codes**
- `200 OK`
- `500 Internal Server Error`: empty body

---

### 9.4 `GET /api/v1/market-events/{id}`
Returns event by ID.

**Status codes**
- `200 OK`
- `404 Not Found`
- `500 Internal Server Error`: empty body

---

### 9.5 `GET /api/v1/market-events/symbol/{symbol}?start=...&end=...`
Returns events by symbol and time range.

Query params:
- `start`: ISO-8601 datetime (parsed into `Instant`)
- `end`: ISO-8601 datetime

Example:
`/api/v1/market-events/symbol/NQ?start=2025-12-31T00:00:00Z&end=2025-12-31T23:59:59Z`

---

### 9.6 `GET /api/v1/market-events/symbol/{symbol}/timeframe/{timeframe}?start=...&end=...`
Same as above, with timeframe filter.

---

### 9.7 `PUT /api/v1/market-events/{id}`
Updates event.

**Status codes**
- `200 OK`
- `404 Not Found`
- `500 Internal Server Error`: empty body

---

### 9.8 `DELETE /api/v1/market-events/{id}`
Deletes event.

**Status codes**
- `204 No Content`
- `404 Not Found`
- `500 Internal Server Error`: empty body

---

### 9.9 `GET /api/v1/market-events/health`
Health check returning a map.

Success response:
```json
{
  "status": "UP",
  "service": "CoreMarketEventService",
  "totalEvents": 123,
  "timestamp": "2025-12-31T10:00:00Z"
}
```

Failure response (`503 Service Unavailable`):
```json
{
  "status": "DOWN",
  "service": "CoreMarketEventService",
  "error": "Error message",
  "timestamp": "2025-12-31T10:00:00Z"
}
```

---

## 10) API v1 — AI Conversation Management

This API manages conversation *state* (conversation IDs, turn history, expiry). It is separate from the v2 `ChatSession` API.

### 10.1 AIConversation JSON shape
```json
{
  "id": "...",
  "conversationId": "uuid",
  "symbol": "NQ",
  "conversationType": "TRADE_ANALYSIS",
  "userId": "user123",

  "tradeId": "optional",
  "entityType": "TRADE",
  "entityId": "...",
  "entitySnapshot": {},

  "turns": [
    {
      "responseId": "resp_...",
      "requestId": "req_...",
      "userInputSummary": "...",
      "aiOutputSummary": "...",
      "model": "gpt-4o",
      "timestamp": "2025-12-31T10:00:00Z",
      "tokensUsed": 123,
      "metadata": {}
    }
  ],

  "status": "ACTIVE",
  "createdAt": "2025-12-31T10:00:00Z",
  "lastActivityAt": "2025-12-31T10:05:00Z",
  "expiresAt": "2026-01-01T10:00:00Z",

  "metadata": {},
  "contextData": {},
  "userPreferences": {},
  "tags": ["high-confidence"]
}
```

---

### 10.2 `POST /api/v1/conversations`
Creates a conversation session.

**Request body: `CreateConversationRequest`**
```json
{
  "conversationType": "TRADE_ANALYSIS",
  "symbol": "NQ",
  "userId": "user123",
  "expiryHours": 24
}
```

**Response**: `AIConversation`

---

### 10.3 `GET /api/v1/conversations/{conversationId}`
Returns conversation by `conversationId`.

**Status codes**
- `200 OK`
- `404 Not Found`

---

### 10.4 `GET /api/v1/conversations/{conversationId}/stats`
Returns a small stats map.

Example response:
```json
{
  "conversationId": "uuid",
  "turnCount": 3,
  "createdAt": "2025-12-31T10:00:00Z",
  "lastActivity": "2025-12-31T10:05:00Z",
  "status": "ACTIVE",
  "totalTokens": 1234
}
```

**Status codes**
- `200 OK`
- `404 Not Found` (empty object from service)

---

### 10.5 `GET /api/v1/conversations/symbol/{symbol}`
Returns active conversations for a symbol.

**Response**: `AIConversation[]`

---

### 10.6 `POST /api/v1/conversations/{conversationId}/complete`
Marks conversation completed.

Response:
```json
{ "status": "success", "message": "Conversation marked as completed" }
```

---

### 10.7 `POST /api/v1/conversations/cleanup`
Marks expired conversations as EXPIRED.

Response:
```json
{ "cleanedUp": 2, "message": "2 expired conversations cleaned up" }
```

---

## 10A) API v1 — OHLC Data Management

Base path: `/api/v1/ohlc`

**Purpose**: This controller manages OHLC (Open, High, Low, Close) candlestick data for market analysis. It provides endpoints for storing, retrieving, and managing candlestick data across different symbols and timeframes.

**Related documents**: `OHLCData` (MongoDB document)

### 10A.1 `OHLCData` Structure
```json
{
  "id": "mongo-id",
  "symbol": "NQ",
  "timeframe": "15m",
  "timestamp": "2025-12-31T10:00:00Z",
  "epochMillis": 1735639200000,
  "open": 19500.50,
  "high": 19520.75,
  "low": 19495.25,
  "close": 19515.00,
  "volume": 12345
}
```

---

### 10A.2 `POST /api/v1/ohlc`
Creates a new OHLC data record.

**Request body**: `OHLCData`

**Response**: `OHLCData` (201 Created)

---

### 10A.3 `GET /api/v1/ohlc`
Returns all OHLC records (use with caution on large datasets).

**Response**: `OHLCData[]` (200 OK)

---

### 10A.4 `GET /api/v1/ohlc/{id}`
Returns a specific OHLC record by MongoDB `_id`.

**Response**: `OHLCData` (200 OK) or 404 Not Found

---

### 10A.5 `GET /api/v1/ohlc/symbol/{symbol}/timeframe/{timeframe}`
Returns OHLC data for a specific symbol and timeframe within a date range.

**Query parameters**:
- `start`: ISO-8601 timestamp (required)
- `end`: ISO-8601 timestamp (required)

**Example**: `GET /api/v1/ohlc/symbol/NQ/timeframe/15m?start=2025-12-31T00:00:00Z&end=2025-12-31T23:59:59Z`

**Response**: `OHLCData[]` ordered by timestamp ascending

---

### 10A.6 `GET /api/v1/ohlc/symbol/{symbol}/timeframe/{timeframe}/epoch`
Returns OHLC data using epoch milliseconds for the time range (alternative to ISO-8601).

**Query parameters**:
- `startMillis`: epoch milliseconds (required)
- `endMillis`: epoch milliseconds (required)

**Example**: `GET /api/v1/ohlc/symbol/NQ/timeframe/15m/epoch?startMillis=1735689600000&endMillis=1735775999000`

**Response**: `OHLCData[]` ordered by epochMillis ascending

---

### 10A.7 `GET /api/v1/ohlc/symbol/{symbol}/timeframe/{timeframe}/latest`
Returns the most recent OHLC record for a symbol and timeframe.

**Example**: `GET /api/v1/ohlc/symbol/NQ/timeframe/15m/latest`

**Response**: `OHLCData` (200 OK) or 404 Not Found

---

### 10A.8 `PUT /api/v1/ohlc/{id}`
Updates an existing OHLC record.

**Request body**: `OHLCData` (id field will be overridden by path parameter)

**Response**: `OHLCData` (200 OK) or 404 Not Found

---

### 10A.9 `DELETE /api/v1/ohlc/{id}`
Deletes an OHLC record by id.

**Response**: 204 No Content or 404 Not Found

---

### 10A.10 `GET /api/v1/ohlc/health`
Health check endpoint for OHLC service.

**Response**:
```json
{
  "status": "UP",
  "service": "OHLCService",
  "totalRecords": 15234,
  "timestamp": "2025-12-31T10:00:00Z"
}
```

**Status codes**:
- `200 OK` - Service is healthy
- `503 Service Unavailable` - Service is down (with error details)

---

## 10B) API v1 — Transformed Events Management

Base path: `/api/v1/transformed-events`

**Purpose**: This controller manages transformed market events that have been processed and enriched from raw market events. These events represent analyzed market conditions, patterns, and potential trade signals.

**Related documents**: `TransformedEvent` (MongoDB document)

### 10B.1 `TransformedEvent` Structure
```json
{
  "id": "mongo-id",
  "symbol": "NQ",
  "timeframe": "15m",
  "uniqueEventCode": "NQ_15m_FVG_CE_20251231100000",
  "eventTs": "2025-12-31T10:00:00Z",
  "isTradeSignal": true,
  "eventType": "FVG_CE",
  "price": 19515.00,
  "confidence": 0.85,
  "metadata": {
    "pattern": "Fair Value Gap",
    "direction": "BULLISH",
    "additionalData": {}
  }
}
```

---

### 10B.2 `POST /api/v1/transformed-events`
Creates a new transformed event record.

**Request body**: `TransformedEvent`

**Response**: `TransformedEvent` (201 Created)

---

### 10B.3 `GET /api/v1/transformed-events`
Returns all transformed events (use with caution on large datasets).

**Response**: `TransformedEvent[]` (200 OK)

---

### 10B.4 `GET /api/v1/transformed-events/{id}`
Returns a specific transformed event by MongoDB `_id`.

**Response**: `TransformedEvent` (200 OK) or 404 Not Found

---

### 10B.5 `GET /api/v1/transformed-events/symbol/{symbol}`
Returns transformed events for a specific symbol within a date range.

**Query parameters**:
- `start`: ISO-8601 timestamp (required)
- `end`: ISO-8601 timestamp (required)

**Example**: `GET /api/v1/transformed-events/symbol/NQ?start=2025-12-31T00:00:00Z&end=2025-12-31T23:59:59Z`

**Response**: `TransformedEvent[]` ordered by eventTs descending

---

### 10B.6 `GET /api/v1/transformed-events/symbol/{symbol}/timeframe/{timeframe}`
Returns transformed events for a specific symbol and timeframe within a date range.

**Query parameters**:
- `start`: ISO-8601 timestamp (required)
- `end`: ISO-8601 timestamp (required)

**Example**: `GET /api/v1/transformed-events/symbol/NQ/timeframe/15m?start=2025-12-31T00:00:00Z&end=2025-12-31T23:59:59Z`

**Response**: `TransformedEvent[]` ordered by eventTs descending

---

### 10B.7 `GET /api/v1/transformed-events/uec/{uniqueEventCode}`
Returns transformed events matching a specific unique event code within a date range.

**Query parameters**:
- `start`: ISO-8601 timestamp (required)
- `end`: ISO-8601 timestamp (required)

**Example**: `GET /api/v1/transformed-events/uec/NQ_15m_FVG_CE_20251231100000?start=2025-12-31T00:00:00Z&end=2025-12-31T23:59:59Z`

**Response**: `TransformedEvent[]` ordered by eventTs descending

---

### 10B.8 `GET /api/v1/transformed-events/trade-signals/symbol/{symbol}/timeframe/{timeframe}`
Returns only events marked as trade signals for a specific symbol and timeframe.

**Query parameters**:
- `start`: ISO-8601 timestamp (required)
- `end`: ISO-8601 timestamp (required)

**Example**: `GET /api/v1/transformed-events/trade-signals/symbol/NQ/timeframe/15m?start=2025-12-31T00:00:00Z&end=2025-12-31T23:59:59Z`

**Response**: `TransformedEvent[]` where `isTradeSignal=true`, ordered by eventTs descending

---

### 10B.9 `PUT /api/v1/transformed-events/{id}`
Updates an existing transformed event record.

**Request body**: `TransformedEvent` (id field will be overridden by path parameter)

**Response**: `TransformedEvent` (200 OK) or 404 Not Found

---

### 10B.10 `DELETE /api/v1/transformed-events/{id}`
Deletes a transformed event by id.

**Response**: 204 No Content or 404 Not Found

---

### 10B.11 `GET /api/v1/transformed-events/health`
Health check endpoint for Transformed Events service.

**Response**:
```json
{
  "status": "UP",
  "service": "TransformedEventService",
  "totalEvents": 8567,
  "timestamp": "2025-12-31T10:00:00Z"
}
```

**Status codes**:
- `200 OK` - Service is healthy
- `503 Service Unavailable` - Service is down (with error details)

---

## 11) UI Integration Checklist (Practical)

- Use ISO-8601 timestamps (UTC) for `Instant` query params (`start`, `end`) and display.
- Implement retry/backoff on `502` if you integrate endpoints that throw `AIClientException` (global handler). Many AI endpoints instead return `500` with empty body; treat both as retryable *only if your UX wants it*.
- For flexible object fields (`marketData`, `meta`, `parameters`, `contextData`), implement a JSON editor or key/value editor in admin UIs.
- Treat `ChatSession` (v2) and `AIConversation` (v1) as different concepts:
  - v2 ChatSession is end-user chat history
  - v1 AIConversation is backend conversation state (response IDs, expiry, turns)

---

## 12) Source of Truth

This doc is generated from these code locations:
- Controllers:
  - `com.trade.app.controller.UnifiedAIController`
  - `com.trade.app.controller.SchedulerConfigController`
  - `com.trade.app.controller.OperationLogController`
  - `com.trade.app.controller.TradeFinderController`
  - `com.trade.app.controller.UiMetadataController`
  - `com.trade.app.controller.CoreMarketEventController`
  - `com.trade.app.controller.AIConversationController`
  - `com.trade.app.controller.OHLCDataController`
  - `com.trade.app.controller.TransformedEventController`
- DTOs/Documents:
  - `com.trade.app.domain.dto.*`
  - `com.trade.app.persistence.mongo.document.*`
  - `com.trade.app.openai.dto.*`
- Constants:
  - `com.trade.app.util.Constants`

---

## 13) API v2 — Operation Logs (UI)

Base path: `/api/v2/operations`

### 13.1 `GET /api/v2/operations/logs`
Query operation logs to show execution history in the UI.

Common query params:
- `operationId`: correlation id (UUID string). If provided, returns all records for that operation.
- `operationType`: e.g. `SCHEDULER_EXECUTION`, `TRADE_FINDER_RUN`, `TRADE_FINDER_TRIGGER`, `AI_ANALYZE`, `AI_ANALYZE_TRADE`, `TRADE_STATUS_UPDATE`
- `status`: `IN_PROGRESS | SUCCESS | FAILED`
- `source`: e.g. `SCHEDULER | MANUAL | API`
- `from`, `to`: ISO-8601 timestamps
- `limit`: default `200`, max `1000`

**Response**: `OperationLog[]`
```json
[
  {
    "id": "mongo-id",
    "operationId": "uuid",
    "operationType": "SCHEDULER_EXECUTION",
    "source": "SCHEDULER",
    "status": "SUCCESS",
    "title": "Scheduler execution: trade-finder-nq",
    "message": "Scheduler execution completed",
    "error": null,
    "startedAt": "2025-12-31T10:00:00Z",
    "endedAt": "2025-12-31T10:00:02Z",
    "durationMs": 2100,
    "metadata": {"schedulerName": "trade-finder-nq"},
    "events": [
      {"timestamp": "2025-12-31T10:00:00Z", "level": "INFO", "message": "Scheduler execution started", "data": {"schedulerName": "trade-finder-nq"}}
    ]
  }
]
```

### 13.2 `GET /api/v2/operations/{operationId}`
Returns the latest `OperationLog` record for a correlation id.

### 13.3 `GET /api/v2/operations/logs/{id}`
Returns a specific log record by Mongo `_id`.

---

## 14) API v2 — UI Constants

Base path: `/api/v2/ui`

### 15.1 `GET /api/v2/ui/constants`
Returns allowed values (from backend constants) for dropdowns.

Example response:
```json
{
  "tradeStatuses": ["IDENTIFIED", "ALERTED", "EXPIRED", "CANCELLED", "TAKEN", "INVALIDATED"],
  "tradeDirections": ["LONG", "SHORT"],
  "tradeSignalStatuses": ["TRADE_IDENTIFIED", "NO_SETUP", "INSUFFICIENT_DATA", "ERROR"],
  "entryZoneTypes": ["FVG_CE", "IFVG", "OB", "BREAKER", "MITIGATION"],
  "alertTypes": ["CALL_SMS_TELEGRAM", "SMS_TELEGRAM", "LOG_ONLY"],
  "conversationStatuses": ["ACTIVE", "COMPLETED", "EXPIRED"],
  "conversationTypes": ["TRADE_FOLLOWUP", "TRADE_ANALYSIS", "MARKET_ANALYSIS", "WORKFLOW"],
  "conversationEntityTypes": ["TRADE", "SYMBOL", "PATTERN"]
}
```
