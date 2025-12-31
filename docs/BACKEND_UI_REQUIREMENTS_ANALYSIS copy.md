# Backend-UI Requirements Analysis
## AI Trade Finder Spring Boot Service

**Generated:** December 28, 2025  
**Purpose:** Comprehensive analysis of backend capabilities vs UI requirements

---

## Executive Summary

This document provides a detailed analysis of the AI Trade Finder Spring Boot backend service, comparing its current capabilities against the UI requirements. The analysis identifies:

1. **What the backend CAN fulfill** - Existing features and endpoints
2. **What NEEDS to be added** - Missing functionality required by the UI
3. **Implementation roadmap** - Prioritized development recommendations

---

## Table of Contents

- [Current Backend Capabilities](#current-backend-capabilities)
- [UI Requirements Analysis](#ui-requirements-analysis)
- [Gap Analysis](#gap-analysis)
- [Required Backend Additions](#required-backend-additions)
- [Implementation Roadmap](#implementation-roadmap)

---

## Current Backend Capabilities

### 1. Existing REST Controllers

#### AIAnalysisController (`/api/v1/ai`)

**Available Endpoints:**

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/v1/ai/analyze` | AI-powered trade analysis | ✅ Active |
| POST | `/api/v1/ai/analyze/{model}` | Analysis with specific model | ✅ Active |
| POST | `/api/v1/ai/quick-analyze` | Simplified analysis request | ✅ Active |
| POST | `/api/v1/ai/workflow` | Execute AI workflow with market data | ✅ Active |
| GET | `/api/v1/ai/health` | AI service health check | ✅ Active |
| GET | `/api/v1/ai/prompts` | Get available predefined prompts | ✅ Active |
| GET | `/api/v1/ai/data-sources` | Get available data sources | ✅ Active |
| GET | `/api/v1/ai/data-sources/health` | Data sources health status | ✅ Active |

**Capabilities:**
- AI-powered trade analysis using OpenAI
- Multi-model support (GPT-4o, o1, o1-mini)
- Predefined prompt templates (day_analysis, swing_trade, risk_assessment, etc.)
- Market data integration with AI workflows
- Multi-timeframe analysis
- Health monitoring

#### CoreMarketEventController (`/api/v1/market-events`)

**Available Endpoints:**

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/v1/market-events` | Create new market event | ✅ Active |
| GET | `/api/v1/market-events` | Get all market events | ✅ Active |
| GET | `/api/v1/market-events/{id}` | Get event by ID | ✅ Active |
| GET | `/api/v1/market-events/symbol/{symbol}` | Get events by symbol and time range | ✅ Active |
| GET | `/api/v1/market-events/symbol/{symbol}/timeframe/{timeframe}` | Get events by symbol, timeframe, and time range | ✅ Active |
| PUT | `/api/v1/market-events/{id}` | Update market event | ✅ Active |
| DELETE | `/api/v1/market-events/{id}` | Delete market event | ✅ Active |
| GET | `/api/v1/market-events/health` | Service health check | ✅ Active |

**Capabilities:**
- CRUD operations for market events
- Time-based queries (date ranges)
- Symbol-based filtering
- Timeframe-based filtering
- Health monitoring

### 2. Data Models

#### MongoDB Documents

**CoreMarketEvent:**
```java
{
  "id": String,
  "symbol": String,
  "timeframe": String,
  "indicatorName": String,
  "rawMessage": String,
  "ingestedTs": Instant,
  "meta": Map<String, Object>,
  "queued": boolean,
  "transformAttempts": int
}
```

**TradeEventDocument:**
```java
{
  "id": String,
  "type": String,
  "payload": String,
  "createdAt": Instant
}
```

**OHLCData:**
```java
{
  "id": String,
  "symbol": String,
  "timeframe": String,
  "epochMillis": Long,
  "timestamp": Instant,
  "open": BigDecimal,
  "high": BigDecimal,
  "low": BigDecimal,
  "close": BigDecimal,
  "volume": Long,
  "source": String
}
```

### 3. Available Services

- **AIClientService** - OpenAI integration for trade analysis
- **AIWorkflowService** - Multi-step AI workflows with market data
- **DataSourceFactory** - Multiple data source management
- **CoreMarketEventDataSource** - Market event data retrieval

### 4. Repository Layer

- **CoreMarketEventRepository** - MongoDB queries for market events
- **TradeEventRepository** - Trade event persistence
- **OHLCRepository** - OHLC candlestick data queries

---

## UI Requirements Analysis

### Dashboard 1: Alerts Dashboard

**UI Requirements:**
```json
{
  "id": "string",
  "type": "string",
  "message": "string",
  "timestamp": "datetime",
  "status": "string"
}
```

**Backend Status:** ⚠️ **MISSING** - No alert management system

---

### Dashboard 2: Chat Dashboard

**UI Requirements:**
```json
{
  "id": "string",
  "sender": "string",
  "message": "string",
  "timestamp": "datetime"
}
```

**Backend Status:** ⚠️ **MISSING** - No chat system (Could leverage AI analysis for Q&A)

---

### Dashboard 3: System Dashboard

**UI Requirements:**
```json
{
  "cpuUsage": "number",
  "memoryUsage": "number",
  "uptime": "number",
  "logs": [
    {
      "id": "string",
      "event": "string",
      "timestamp": "datetime"
    }
  ]
}
```

**Backend Status:** ⚠️ **PARTIALLY AVAILABLE**
- ✅ Health check endpoints exist
- ⚠️ Missing: System metrics (CPU, memory, uptime)
- ⚠️ Missing: System event logs

---

### Dashboard 4: Trades Dashboard

**UI Requirements:**
```json
{
  "id": "string",
  "symbol": "string",
  "price": "number",
  "quantity": "number",
  "timestamp": "datetime"
}

// Filters
{
  "dateRange": { "start": "datetime", "end": "datetime" },
  "symbol": "string",
  "priceRange": { "min": "number", "max": "number" }
}
```

**Backend Status:** ⚠️ **PARTIALLY AVAILABLE**
- ✅ CoreMarketEvent data (similar structure)
- ✅ Symbol filtering exists
- ✅ Date range filtering exists
- ⚠️ Missing: Actual "Trade" entity (buy/sell transactions)
- ⚠️ Missing: Price range filtering
- ⚠️ Missing: Pagination support

---

### Component: Metric Card

**UI Requirements:**
```json
{
  "label": "string",
  "value": "any",
  "unit": "string"
}
```

**Backend Status:** ✅ **CAN BE FULFILLED**
- Can aggregate data from existing endpoints
- Health endpoints provide some metrics

---

### Component: Recent Trades Widget

**UI Requirements:**
```json
[
  {
    "id": "string",
    "symbol": "string",
    "price": "number",
    "quantity": "number",
    "timestamp": "datetime"
  }
]
```

**Backend Status:** ⚠️ **PARTIALLY AVAILABLE**
- ✅ Can use market events as proxy
- ⚠️ Missing: Actual trade execution data

---

### Component: Sidebar & Topbar

**UI Requirements:**
```json
// Navigation items
{
  "id": "string",
  "label": "string",
  "link": "string"
}

// User info
{
  "name": "string",
  "avatar": "string"
}

// Notifications
{
  "id": "string",
  "message": "string",
  "timestamp": "datetime"
}
```

**Backend Status:** ⚠️ **MISSING**
- ⚠️ No user management system
- ⚠️ No authentication/authorization
- ⚠️ No notification system

---

### Component: Trades Table

**UI Requirements:**
```json
{
  "trades": [],
  "pagination": {
    "currentPage": "number",
    "totalPages": "number",
    "pageSize": "number"
  }
}
```

**Backend Status:** ⚠️ **MISSING PAGINATION**
- ✅ Can retrieve market events
- ⚠️ Missing: Pagination support
- ⚠️ Missing: Sorting configuration

---

## Gap Analysis

### ✅ What Backend CAN Fulfill (Existing)

1. **Market Events Dashboard** (maps to "Trades Dashboard" partially)
   - GET market events by symbol
   - GET market events by timeframe
   - Date range filtering
   - Symbol-based queries

2. **AI Analysis Dashboard** (could power "Chat Dashboard")
   - AI-powered analysis
   - Quick analysis queries
   - Pre-defined prompts
   - Custom analysis requests

3. **Basic Health Metrics**
   - Service health status
   - Data source health
   - Timestamp tracking

4. **OHLC Data**
   - Candlestick price data
   - Multi-timeframe support
   - Symbol-based queries

---

### ⚠️ What NEEDS to Be Added (Missing)

#### HIGH PRIORITY - Core Functionality

1. **Alerts Management System**
   - Alert CRUD endpoints
   - Alert types/categories
   - Alert status management (read/unread)
   - Real-time alert generation
   - Alert history

2. **Trade Execution Tracking**
   - Trade entity (buy/sell)
   - Trade status (pending, executed, cancelled)
   - Trade price and quantity
   - Trade type (market, limit, stop)
   - P&L tracking

3. **Pagination Support**
   - Pageable request handling
   - Page metadata (total, size, current)
   - Sorting configuration
   - Universal pagination across all list endpoints

4. **Advanced Filtering**
   - Price range filtering
   - Multi-field filtering
   - Dynamic filter builders
   - Saved filter presets

#### MEDIUM PRIORITY - Enhanced Features

5. **User Management**
   - User authentication
   - User profiles
   - User preferences
   - Session management

6. **Notification System**
   - Notification creation
   - Notification types
   - Read/unread status
   - Notification history
   - Real-time push notifications

7. **System Monitoring**
   - CPU usage metrics
   - Memory usage metrics
   - System uptime
   - Application logs endpoint
   - Performance metrics

8. **Chat/Conversation System**
   - Chat message storage
   - Conversation history
   - Multi-user chat support
   - AI assistant integration

#### LOW PRIORITY - Nice to Have

9. **Dashboard Analytics**
   - Aggregated metrics
   - Time-series statistics
   - Performance analytics
   - Custom dashboard configurations

10. **Export Capabilities**
    - CSV export
    - PDF reports
    - Excel export
    - Custom report templates

11. **Webhook Support**
    - Webhook configuration
    - Event-based triggers
    - External integrations

12. **Audit Logging**
    - User action logs
    - System event logs
    - Compliance tracking

---

## Required Backend Additions

### 1. Alert Management System

#### New Entities

**Alert.java**
```java
@Document("alerts")
public class Alert {
    @Id
    private String id;
    
    private String type; // TRADE_ALERT, SYSTEM_ALERT, PRICE_ALERT
    private String message;
    private String severity; // INFO, WARNING, CRITICAL
    private Instant timestamp;
    private String status; // UNREAD, READ, DISMISSED
    private String userId; // Optional: for user-specific alerts
    private String symbol; // Optional: related symbol
    private Map<String, Object> metadata;
}
```

#### New Controller

**AlertController.java**
```java
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    
    // GET /api/v1/alerts - Get all alerts (with pagination)
    // GET /api/v1/alerts/{id} - Get alert by ID
    // POST /api/v1/alerts - Create new alert
    // PUT /api/v1/alerts/{id} - Update alert
    // PUT /api/v1/alerts/{id}/status - Update alert status
    // DELETE /api/v1/alerts/{id} - Delete alert
    // GET /api/v1/alerts/unread - Get unread alerts
    // GET /api/v1/alerts/user/{userId} - Get user-specific alerts
}
```

#### Repository

**AlertRepository.java**
```java
public interface AlertRepository extends MongoRepository<Alert, String> {
    Page<Alert> findByStatus(String status, Pageable pageable);
    List<Alert> findByUserIdAndStatus(String userId, String status);
    Page<Alert> findByTypeAndTimestampBetween(String type, Instant start, Instant end, Pageable pageable);
}
```

---

### 2. Trade Execution System

#### New Entity

**Trade.java**
```java
@Document("trades")
public class Trade {
    @Id
    private String id;
    
    private String symbol;
    private String type; // BUY, SELL
    private String orderType; // MARKET, LIMIT, STOP
    private BigDecimal price;
    private BigDecimal quantity;
    private String status; // PENDING, EXECUTED, CANCELLED, FAILED
    private Instant timestamp;
    private Instant executedAt;
    private String userId;
    private BigDecimal commission;
    private BigDecimal pnl; // Profit/Loss
    private Map<String, Object> metadata;
}
```

#### New Controller

**TradeController.java**
```java
@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {
    
    // GET /api/v1/trades - Get all trades (with pagination and filters)
    // GET /api/v1/trades/{id} - Get trade by ID
    // POST /api/v1/trades - Create/execute new trade
    // PUT /api/v1/trades/{id} - Update trade
    // DELETE /api/v1/trades/{id} - Cancel/delete trade
    // GET /api/v1/trades/symbol/{symbol} - Get trades by symbol
    // GET /api/v1/trades/recent - Get recent trades
    // GET /api/v1/trades/stats - Get trade statistics
}
```

#### Repository

**TradeRepository.java**
```java
public interface TradeRepository extends MongoRepository<Trade, String> {
    Page<Trade> findAll(Pageable pageable);
    Page<Trade> findBySymbol(String symbol, Pageable pageable);
    Page<Trade> findByTimestampBetween(Instant start, Instant end, Pageable pageable);
    Page<Trade> findBySymbolAndPriceBetween(String symbol, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    List<Trade> findTop10ByOrderByTimestampDesc();
}
```

---

### 3. Pagination Support

#### Common DTO

**PageResponse.java**
```java
@Data
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean first;
    private boolean last;
}
```

#### Update Existing Controllers

- Add `Pageable` parameters to all list endpoints
- Return `PageResponse<T>` instead of `List<T>`
- Add sorting support

---

### 4. System Monitoring

#### New Controller

**SystemMetricsController.java**
```java
@RestController
@RequestMapping("/api/v1/system")
public class SystemMetricsController {
    
    // GET /api/v1/system/metrics - Get system metrics
    // GET /api/v1/system/uptime - Get system uptime
    // GET /api/v1/system/logs - Get system logs
    // GET /api/v1/system/performance - Get performance metrics
}
```

#### Response Model

```java
@Data
public class SystemMetrics {
    private double cpuUsage;
    private double memoryUsage;
    private long uptime;
    private String status;
    private Instant timestamp;
    private Map<String, Object> additionalMetrics;
}
```

---

### 5. Notification System

#### New Entity

**Notification.java**
```java
@Document("notifications")
public class Notification {
    @Id
    private String id;
    
    private String userId;
    private String message;
    private String type; // INFO, WARNING, ERROR, SUCCESS
    private Instant timestamp;
    private String status; // UNREAD, READ
    private String sourceType; // ALERT, TRADE, SYSTEM
    private String sourceId;
    private Map<String, Object> data;
}
```

#### New Controller

**NotificationController.java**
```java
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    
    // GET /api/v1/notifications - Get all notifications
    // GET /api/v1/notifications/unread - Get unread notifications
    // PUT /api/v1/notifications/{id}/read - Mark as read
    // PUT /api/v1/notifications/read-all - Mark all as read
    // DELETE /api/v1/notifications/{id} - Delete notification
}
```

---

### 6. User Management

#### New Entity

**User.java**
```java
@Document("users")
public class User {
    @Id
    private String id;
    
    private String username;
    private String email;
    private String name;
    private String avatar;
    private String role; // USER, ADMIN
    private Instant createdAt;
    private Instant lastLoginAt;
    private Map<String, Object> preferences;
}
```

#### New Controller

**UserController.java**
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    // GET /api/v1/users/me - Get current user
    // PUT /api/v1/users/me - Update current user
    // GET /api/v1/users/{id} - Get user by ID
    // PUT /api/v1/users/me/preferences - Update preferences
}
```

---

### 7. Chat/Conversation System

#### New Entity

**ChatMessage.java**
```java
@Document("chat_messages")
public class ChatMessage {
    @Id
    private String id;
    
    private String conversationId;
    private String sender; // USER, AI_ASSISTANT
    private String message;
    private Instant timestamp;
    private String type; // TEXT, ANALYSIS, QUERY
    private Map<String, Object> metadata;
}
```

#### New Controller

**ChatController.java**
```java
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    
    // POST /api/v1/chat/send - Send message (can integrate with AI)
    // GET /api/v1/chat/conversations - Get all conversations
    // GET /api/v1/chat/conversations/{id} - Get conversation messages
    // DELETE /api/v1/chat/conversations/{id} - Delete conversation
}
```

---

## Implementation Roadmap

### Phase 1: Core Functionality (Week 1-2)

**Priority: CRITICAL**

1. **Trade Management System**
   - Create Trade entity and repository
   - Implement TradeController with full CRUD
   - Add pagination support
   - Add filtering by symbol, date, price range
   - Create recent trades endpoint

2. **Alert System**
   - Create Alert entity and repository
   - Implement AlertController
   - Add alert status management
   - Create alert generation service

3. **Pagination Infrastructure**
   - Create PageResponse DTO
   - Update existing controllers to support pagination
   - Add sorting support across all list endpoints

**Deliverables:**
- `/api/v1/trades` - Full CRUD with pagination
- `/api/v1/alerts` - Full CRUD with status management
- Paginated responses for market-events

---

### Phase 2: Enhanced Features (Week 3-4)

**Priority: HIGH**

1. **System Monitoring**
   - Implement SystemMetricsController
   - Add JVM metrics (CPU, memory, threads)
   - Create system logs endpoint
   - Add uptime tracking

2. **Notification System**
   - Create Notification entity and repository
   - Implement NotificationController
   - Integrate with Alert system
   - Create notification generation service

3. **Advanced Filtering**
   - Add price range filtering to trades
   - Implement dynamic filter builder
   - Add saved filter presets

**Deliverables:**
- `/api/v1/system/metrics` - System monitoring
- `/api/v1/notifications` - Notification management
- Enhanced filtering across all endpoints

---

### Phase 3: User Features (Week 5-6)

**Priority: MEDIUM**

1. **User Management**
   - Create User entity and repository
   - Implement UserController
   - Add user preferences
   - Basic authentication setup

2. **Chat System**
   - Create ChatMessage entity
   - Implement ChatController
   - Integrate with AI analysis
   - Add conversation history

3. **Dashboard Analytics**
   - Create analytics endpoints
   - Add aggregated metrics
   - Implement dashboard configuration

**Deliverables:**
- `/api/v1/users` - User management
- `/api/v1/chat` - Chat system
- `/api/v1/analytics` - Dashboard analytics

---

### Phase 4: Advanced Features (Week 7-8)

**Priority: LOW**

1. **Export Capabilities**
   - CSV export for trades
   - PDF reports
   - Excel export

2. **Webhook Support**
   - Webhook configuration
   - Event triggers
   - External integrations

3. **Audit Logging**
   - User action logs
   - System event logs
   - Compliance tracking

**Deliverables:**
- Export functionality
- Webhook system
- Comprehensive audit logs

---

## API Endpoint Summary

### Current Endpoints (Available)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/ai/analyze` | POST | AI analysis |
| `/api/v1/ai/workflow` | POST | AI workflow execution |
| `/api/v1/market-events` | GET | Get market events |
| `/api/v1/market-events/{id}` | GET | Get event by ID |
| `/api/v1/market-events/symbol/{symbol}` | GET | Get events by symbol |

### Required New Endpoints

| Endpoint | Method | Purpose | Priority |
|----------|--------|---------|----------|
| `/api/v1/trades` | GET | Get all trades (paginated) | HIGH |
| `/api/v1/trades/{id}` | GET | Get trade by ID | HIGH |
| `/api/v1/trades` | POST | Create/execute trade | HIGH |
| `/api/v1/trades/recent` | GET | Get recent trades | HIGH |
| `/api/v1/alerts` | GET | Get all alerts (paginated) | HIGH |
| `/api/v1/alerts/{id}` | GET | Get alert by ID | HIGH |
| `/api/v1/alerts` | POST | Create alert | HIGH |
| `/api/v1/alerts/unread` | GET | Get unread alerts | HIGH |
| `/api/v1/system/metrics` | GET | Get system metrics | MEDIUM |
| `/api/v1/system/logs` | GET | Get system logs | MEDIUM |
| `/api/v1/notifications` | GET | Get notifications | MEDIUM |
| `/api/v1/notifications/unread` | GET | Get unread notifications | MEDIUM |
| `/api/v1/users/me` | GET | Get current user | MEDIUM |
| `/api/v1/chat/send` | POST | Send chat message | LOW |
| `/api/v1/chat/conversations` | GET | Get conversations | LOW |

---

## Configuration Updates Required

### 1. Add Spring Boot Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```properties
# application.properties
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=always
management.metrics.enable.jvm=true
management.metrics.enable.system=true
```

### 2. Add Pagination Support

Already using Spring Data MongoDB which supports pagination out of the box.

### 3. Add Validation

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 4. Add Security (Optional for Phase 3)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## Testing Requirements

### Unit Tests Needed

1. **TradeController** - All CRUD operations
2. **AlertController** - Alert management
3. **NotificationController** - Notification handling
4. **SystemMetricsController** - Metrics retrieval
5. **TradeRepository** - Custom query methods
6. **AlertRepository** - Custom query methods

### Integration Tests Needed

1. Trade creation and retrieval flow
2. Alert generation and notification flow
3. Pagination across all endpoints
4. Filtering combinations
5. AI workflow integration with new entities

---

## Documentation Requirements

### API Documentation

1. Add Swagger/OpenAPI documentation
2. Document all new endpoints
3. Provide request/response examples
4. Include error codes and messages

### Developer Documentation

1. Architecture overview
2. Data flow diagrams
3. Integration guides
4. Deployment instructions

---

## Performance Considerations

### Database Indexes

**Required Indexes:**

```javascript
// Trades collection
db.trades.createIndex({ "symbol": 1, "timestamp": -1 })
db.trades.createIndex({ "userId": 1, "timestamp": -1 })
db.trades.createIndex({ "status": 1 })
db.trades.createIndex({ "symbol": 1, "price": 1 })

// Alerts collection
db.alerts.createIndex({ "status": 1, "timestamp": -1 })
db.alerts.createIndex({ "userId": 1, "status": 1 })
db.alerts.createIndex({ "type": 1, "timestamp": -1 })

// Notifications collection
db.notifications.createIndex({ "userId": 1, "status": 1 })
db.notifications.createIndex({ "timestamp": -1 })

// Chat messages collection
db.chat_messages.createIndex({ "conversationId": 1, "timestamp": 1 })
```

### Caching Strategy

1. Cache recent trades (5-minute TTL)
2. Cache system metrics (1-minute TTL)
3. Cache user preferences (no expiration, invalidate on update)
4. Cache dashboard analytics (5-minute TTL)

---

## Security Considerations

### Authentication

- Implement JWT-based authentication
- Add role-based access control (RBAC)
- Secure sensitive endpoints

### Authorization

- User-specific data access
- Admin-only endpoints
- Rate limiting for public endpoints

### Data Protection

- Encrypt sensitive user data
- Implement audit logging
- Add CORS configuration for UI

---

## Conclusion

### Summary

The AI Trade Finder backend has a **solid foundation** with:
- ✅ AI analysis capabilities
- ✅ Market event management
- ✅ OHLC data handling
- ✅ Multi-timeframe support

However, to fully support the UI requirements, the following **critical additions** are needed:
1. **Trade Management System** (buy/sell transactions)
2. **Alert System** (notifications and alerts)
3. **Pagination Support** (across all list endpoints)
4. **System Monitoring** (metrics and logs)
5. **User Management** (profiles and preferences)
6. **Notification System** (real-time notifications)

### Estimated Development Time

- **Phase 1 (Core):** 2 weeks
- **Phase 2 (Enhanced):** 2 weeks
- **Phase 3 (User Features):** 2 weeks
- **Phase 4 (Advanced):** 2 weeks

**Total: 8 weeks** for complete implementation

### Recommended Approach

1. Start with **Phase 1** (Trade + Alert systems with pagination)
2. Test thoroughly with UI integration
3. Proceed to **Phase 2** (System monitoring and notifications)
4. Add **Phase 3** (User management and chat) based on business priority
5. Consider **Phase 4** as nice-to-have features

---

## Appendix

### A. Useful MongoDB Queries

```javascript
// Get recent trades
db.trades.find().sort({ timestamp: -1 }).limit(10)

// Get unread alerts
db.alerts.find({ status: "UNREAD" })

// Get trades by symbol in date range
db.trades.find({
  symbol: "AAPL",
  timestamp: { $gte: ISODate("2025-01-01"), $lte: ISODate("2025-01-31") }
})

// Get system events
db.system_logs.find({ event: "ERROR" }).sort({ timestamp: -1 })
```

### B. Sample Request/Response

**Create Trade:**
```json
POST /api/v1/trades
{
  "symbol": "AAPL",
  "type": "BUY",
  "orderType": "MARKET",
  "quantity": 10,
  "price": 185.50
}

Response:
{
  "id": "507f1f77bcf86cd799439011",
  "symbol": "AAPL",
  "type": "BUY",
  "orderType": "MARKET",
  "quantity": 10,
  "price": 185.50,
  "status": "EXECUTED",
  "timestamp": "2025-12-28T10:30:00Z",
  "executedAt": "2025-12-28T10:30:01Z"
}
```

**Get Trades with Pagination:**
```json
GET /api/v1/trades?page=0&size=20&sort=timestamp,desc

Response:
{
  "content": [...],
  "currentPage": 0,
  "totalPages": 5,
  "totalElements": 100,
  "pageSize": 20,
  "first": true,
  "last": false
}
```

---

**Document Version:** 1.0  
**Last Updated:** December 28, 2025  
**Author:** AI Trade Finder Team
