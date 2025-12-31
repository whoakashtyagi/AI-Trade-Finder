# AI Trade Finder - Implementation Guide

## Architecture Overview

### Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Layer                          │
├──────────────────┬──────────────────┬──────────────────────┤
│ TradeFinderCtrl  │ UnifiedAICtrl    │ SchedulerConfigCtrl  │
│ (V1 API)         │ (V2 AI/Chat)     │ (V2 Schedulers)      │
└────────┬─────────┴────────┬─────────┴────────┬─────────────┘
         │                  │                   │
         ▼                  ▼                   ▼
┌─────────────────┐  ┌──────────────┐  ┌──────────────────┐
│AITradeFinderSvc │  │AIClientSvc   │  │DynamicScheduler  │
│(Core Logic)     │  │(OpenAI API)  │  │Service           │
└────────┬────────┘  └──────┬───────┘  └────────┬─────────┘
         │                  │                     │
         ▼                  ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    MongoDB Collections                       │
├──────────────────┬─────────────────┬─────────────────────────┤
│identified_trades │ chat_sessions   │ scheduler_configs       │
│transformed_events│ ohlc_data       │ core_market_events      │
└──────────────────┴─────────────────┴─────────────────────────┘
```

---

## Key Design Patterns

### 1. Dynamic Scheduler Pattern

**Problem**: Hard-coded `@Scheduled` annotations require code changes and redeployment

**Solution**: Database-backed scheduler configuration with runtime management

**Implementation**:
```java
@Service
public class DynamicSchedulerService {
    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    public void scheduleTask(SchedulerConfig config) {
        ScheduledFuture<?> future = switch (config.getScheduleType()) {
            case CRON -> taskScheduler.schedule(
                createRunnable(config),
                new CronTrigger(config.getScheduleExpression())
            );
            case FIXED_RATE -> taskScheduler.scheduleAtFixedRate(
                createRunnable(config),
                Duration.ofMillis(Long.parseLong(config.getScheduleExpression()))
            );
            case FIXED_DELAY -> taskScheduler.scheduleWithFixedDelay(
                createRunnable(config),
                Duration.ofMillis(Long.parseLong(config.getScheduleExpression()))
            );
        };
        scheduledTasks.put(config.getName(), future);
    }
}
```

**Benefits**:
- Zero downtime configuration changes
- A/B testing of different schedules
- Emergency disable without deployment
- Per-symbol custom schedules

---

### 2. Chat Session Pattern

**Problem**: Single-shot AI requests lose context and can't have follow-up discussions

**Solution**: Session-based conversation tracking with MongoDB

**Implementation**:
```java
@Document(collection = "chat_sessions")
public class ChatSession {
    private String id;
    private String title;
    private String tradeId; // Link to specific trade
    private List<ChatMessage> messages;
    private Map<String, Object> context;
    
    @Data
    public static class ChatMessage {
        private String role; // user/assistant/system
        private String content;
        private LocalDateTime timestamp;
    }
}
```

**Usage Flow**:
1. User finds interesting trade
2. Creates chat session linked to trade ID
3. Asks questions - AI has full trade context
4. System maintains conversation history
5. Session can be resumed later

---

### 3. Structured Output Pattern

**Problem**: Parsing unstructured AI responses is fragile and error-prone

**Solution**: OpenAI's native JSON structured output mode

**Implementation**:
```java
@PostMapping("/analyze/trade")
public ResponseEntity<TradeSignalResponseDTO> analyzeTradeStructured(
    @RequestBody AIRequestDTO request
) {
    // Add response format for structured output
    String systemPrompt = """
        You must respond with valid JSON matching this schema:
        {
          "status": "TRADE_IDENTIFIED|NO_SETUP|...",
          "direction": "LONG|SHORT",
          "confidence": 0-100,
          "entry": {"zone_type": "...", "price": ...},
          ...
        }
        """;
    
    AIRequestDTO structuredRequest = AIRequestDTO.builder()
        .systemInstructions(systemPrompt)
        .userMessage(request.getUserMessage())
        .responseFormat("json_object") // OpenAI structured mode
        .build();
        
    String response = aiClientService.chat(structuredRequest);
    return objectMapper.readValue(response, TradeSignalResponseDTO.class);
}
```

---

## Data Flow Diagrams

### Trade Identification Flow

```
1. DynamicSchedulerService triggers scheduled task
                ↓
2. AITradeFinderService.findTrades() executes
                ↓
3. Fetches recent TransformedEvent + OHLC data
                ↓
4. Builds TradeFinderPayloadDTO with:
   - Price action (swing highs/lows)
   - Market structure (BOS, CHoCH)
   - Liquidity zones
   - Smart Money indicators (CISD, FVG)
                ↓
5. Loads system prompt from resources/prompts/
                ↓
6. Sends to OpenAI via AIClientService
                ↓
7. Receives TradeSignalResponseDTO (structured JSON)
                ↓
8. Deduplication check (dedupe_key = symbol+direction+zone)
                ↓
9. Saves to identified_trades collection
                ↓
10. Triggers alert (if configured)
```

### Chat Interaction Flow

```
1. POST /api/v2/ai/chat/sessions - Create session
                ↓
2. System creates ChatSession document
                ↓
3. POST /api/v2/ai/chat/sessions/{id}/messages
                ↓
4. Appends user message to session.messages[]
                ↓
5. Builds OpenAI request with full message history
                ↓
6. Sends to OpenAI with conversation context
                ↓
7. Receives AI response
                ↓
8. Appends assistant message to session.messages[]
                ↓
9. Updates session.lastActivityAt
                ↓
10. Returns response to user
```

---

## MongoDB Schema Design

### identified_trades Collection

```javascript
{
  _id: ObjectId("..."),
  symbol: "NQ",
  direction: "LONG",
  confidence: 85,
  status: "ACTIVE",
  entry: {
    zone_type: "FVG_CE",
    zone: "21780-21800",
    price: 21790.0,
    narrative: "Fair Value Gap at key support"
  },
  stop: {
    placement: "Below liquidity sweep",
    price: 21750.0,
    distance_percent: 0.18
  },
  targets: [
    { level: "POC", price: 21850.0, hit: false }
  ],
  dedupe_key: "NQ_LONG_21780-21800_20250101",
  alert_sent: false,
  expired: false,
  expires_at: ISODate("2025-01-01T18:00:00Z"),
  created_at: ISODate("2025-01-01T10:00:00Z")
}
```

### scheduler_configs Collection

```javascript
{
  _id: ObjectId("..."),
  name: "trade-finder-nq",
  description: "NQ trade finder every 5 minutes",
  type: "TRADE_FINDER",
  enabled: true,
  schedule_expression: "0 */5 * * * *",
  schedule_type: "CRON",
  parameters: {
    symbols: ["NQ"],
    timeframes: ["5m", "15m"],
    min_confidence: 70
  },
  handler_bean: "aiTradeFinderService",
  handler_method: "findTrades",
  priority: 10,
  execution_stats: {
    total_executions: 100,
    successful_executions: 98,
    failed_executions: 2,
    last_execution_time: ISODate("..."),
    last_success_time: ISODate("..."),
    last_failure_time: ISODate("..."),
    last_error_message: null
  },
  created_at: ISODate("..."),
  updated_at: ISODate("...")
}
```

### chat_sessions Collection

```javascript
{
  _id: ObjectId("..."),
  title: "Discussion: NQ Long Setup",
  type: "TRADE_DISCUSSION",
  trade_id: ObjectId("..."),
  symbol: "NQ",
  active: true,
  messages: [
    {
      role: "system",
      content: "You are discussing trade: NQ LONG at 21790...",
      timestamp: ISODate("...")
    },
    {
      role: "user",
      content: "Why is this high confidence?",
      timestamp: ISODate("...")
    },
    {
      role: "assistant",
      content: "This setup has 3 key confluences...",
      timestamp: ISODate("...")
    }
  ],
  context: {
    trade_confidence: 85,
    key_levels: [21750, 21800, 21850]
  },
  created_at: ISODate("..."),
  last_activity_at: ISODate("...")
}
```

---

## Configuration Properties

### application.properties

```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/tradedb

# OpenAI
openai.api.key=${OPENAI_API_KEY}
openai.api.model=gpt-4o
openai.api.max-tokens=4000
openai.api.temperature=0.7

# Dynamic Scheduler
scheduler.pool-size=10
scheduler.thread-name-prefix=dynamic-scheduler-
scheduler.await-termination-seconds=30

# Chat Configuration
chat.max-history-messages=50
chat.default-model=gpt-4o
chat.max-tokens=2000
chat.temperature=0.7

# Trade Finder
trade.finder.default.symbols=NQ,ES,YM,RTY
trade.finder.default.timeframes=5m,15m,1h
trade.finder.min.confidence=70
trade.finder.expiration.hours=8

# Alerts
alert.enabled=true
alert.webhook.url=${ALERT_WEBHOOK_URL:}

# Server
server.port=8082
```

---

## Testing Strategy

### Unit Tests

```java
@SpringBootTest
class DynamicSchedulerServiceTest {
    @Autowired DynamicSchedulerService schedulerService;
    @Autowired SchedulerConfigRepository schedulerRepo;
    
    @Test
    void testScheduleTaskFixedRate() {
        SchedulerConfig config = SchedulerConfig.builder()
            .name("test-scheduler")
            .scheduleType(ScheduleType.FIXED_RATE)
            .scheduleExpression("5000")
            .handlerBean("testBean")
            .handlerMethod("testMethod")
            .build();
            
        schedulerService.scheduleTask(config);
        
        assertTrue(schedulerService.isScheduled("test-scheduler"));
        assertEquals(1, schedulerService.getActiveSchedulersCount());
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UnifiedAIControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean AIClientService aiClientService;
    
    @Test
    void testAnalyzeTradeStructured() throws Exception {
        String mockResponse = """
            {
              "status": "TRADE_IDENTIFIED",
              "direction": "LONG",
              "confidence": 85
            }
            """;
        when(aiClientService.chat(any())).thenReturn(mockResponse);
        
        mockMvc.perform(post("/api/v2/ai/analyze/trade")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"symbol\": \"NQ\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("TRADE_IDENTIFIED"));
    }
}
```

---

## Deployment Checklist

### Pre-Deployment

- [ ] Set `OPENAI_API_KEY` environment variable
- [ ] Configure MongoDB connection URI
- [ ] Set `ALERT_WEBHOOK_URL` (if using alerts)
- [ ] Review `application.properties` settings
- [ ] Run all unit and integration tests
- [ ] Build with `mvn clean package`

### Initial Deployment

- [ ] Deploy Spring Boot application
- [ ] Verify MongoDB connectivity
- [ ] Check health endpoint: `GET /actuator/health`
- [ ] Seed default scheduler configurations
- [ ] Test manual trigger: `POST /api/v1/trade-finder/trigger`
- [ ] Verify OpenAI API connectivity

### Post-Deployment

- [ ] Monitor scheduler executions via statistics endpoint
- [ ] Check application logs for errors
- [ ] Test chat session creation and messaging
- [ ] Verify trade persistence in MongoDB
- [ ] Set up alerting/monitoring (Prometheus, Grafana)

---

## Operational Procedures

### Creating a New Scheduler

```bash
# Example: 15-minute ES analyzer
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "es-analyzer-15m",
    "description": "ES analysis every 15 minutes",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "0 */15 * * * *",
    "scheduleType": "CRON",
    "parameters": {
      "symbols": ["ES"],
      "timeframes": ["15m"],
      "minConfidence": 75
    },
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 8
  }'
```

### Emergency Scheduler Disable

```bash
# Stop all trade finding during maintenance
curl -X POST http://localhost:8082/api/v2/schedulers/trade-finder-main/disable
curl -X POST http://localhost:8082/api/v2/schedulers/trade-finder-secondary/disable

# Verify all stopped
curl http://localhost:8082/api/v2/schedulers/active
```

### Monitoring Scheduler Health

```bash
# Check statistics
curl http://localhost:8082/api/v2/schedulers/statistics | jq

# Expected output:
# {
#   "totalSchedulers": 5,
#   "enabledSchedulers": 5,
#   "activeSchedulers": 5,
#   "totalExecutions": 1500,
#   "totalFailures": 2
# }

# View specific scheduler
curl http://localhost:8082/api/v2/schedulers/trade-finder-main | jq .executionStats
```

### Chat Session Management

```bash
# List active sessions
curl http://localhost:8082/api/v2/ai/chat/sessions/active

# Close old sessions (housekeeping)
curl -X POST http://localhost:8082/api/v2/ai/chat/sessions/{sessionId}/close

# Get all sessions for a trade
TRADE_ID="65a1b2c3d4e5f6g7h8i9j0k1"
curl http://localhost:8082/api/v2/ai/chat/trades/$TRADE_ID/sessions
```

---

## Performance Considerations

### Scheduler Pool Sizing

```
Recommended: (CPU Cores * 2) + 1
Example: 4 cores → 10 threads

Set in application.properties:
scheduler.pool-size=10
```

### OpenAI Rate Limits

- GPT-4o: 10,000 TPM (tokens per minute)
- Recommended: Max 6 concurrent schedulers @ 5min intervals
- Use exponential backoff for rate limit errors

### MongoDB Indexing

```javascript
// Critical indexes
db.identified_trades.createIndex({ symbol: 1, created_at: -1 })
db.identified_trades.createIndex({ dedupe_key: 1 }, { unique: true })
db.identified_trades.createIndex({ status: 1, expires_at: 1 })

db.scheduler_configs.createIndex({ name: 1 }, { unique: true })
db.scheduler_configs.createIndex({ enabled: 1 })

db.chat_sessions.createIndex({ trade_id: 1 })
db.chat_sessions.createIndex({ active: 1, last_activity_at: -1 })
```

---

## Troubleshooting Guide

### Issue: Scheduler Not Executing

**Symptoms**: No trades generated, no execution stats

**Diagnosis**:
```bash
# Check if enabled
curl http://localhost:8082/api/v2/schedulers/{name} | jq .enabled

# Check active schedulers
curl http://localhost:8082/api/v2/schedulers/active

# Check application logs
tail -f logs/application.log | grep DynamicSchedulerService
```

**Solutions**:
1. Ensure `enabled: true`
2. Verify cron expression syntax
3. Check handler bean and method exist
4. Review execution stats for error messages

### Issue: Chat Sessions Not Saving Messages

**Symptoms**: Messages disappear, session history incomplete

**Diagnosis**:
```bash
# Check MongoDB connection
curl http://localhost:8082/actuator/health | jq .components.mongo

# Query session directly
mongo tradedb --eval 'db.chat_sessions.findOne()'
```

**Solutions**:
1. Verify MongoDB connectivity
2. Check for document size limits (16MB)
3. Review application logs for persistence errors
4. Ensure chat.max-history-messages not exceeded

### Issue: OpenAI API Errors

**Symptoms**: 429 rate limit, 401 unauthorized, 500 errors

**Diagnosis**:
```bash
# Test API key
curl http://localhost:8082/api/v2/ai/analyze \
  -H "Content-Type: application/json" \
  -d '{"input": "test", "analysisType": "GENERAL"}'
```

**Solutions**:
1. Verify `OPENAI_API_KEY` environment variable
2. Check OpenAI account quota/limits
3. Implement exponential backoff for 429 errors
4. Review max_tokens configuration

---

## Future Enhancements

### Planned Features

1. **WebSocket Support**
   - Real-time trade alerts
   - Live chat updates
   - Streaming AI responses

2. **Alert Enhancements**
   - Discord integration
   - Telegram bot
   - Email notifications
   - SMS alerts

3. **Advanced Analytics**
   - Trade performance tracking
   - Win rate analysis
   - Confidence calibration
   - Backtest integration

4. **Multi-Model Support**
   - Claude integration
   - Anthropic Sonnet
   - Local LLM support (Llama 3)

5. **Authentication & Authorization**
   - JWT token authentication
   - API key management
   - Role-based access control
   - User-specific chat sessions

---

## Contributing Guidelines

1. Follow Spring Boot best practices
2. Write tests for new features (min 80% coverage)
3. Update API documentation
4. Use conventional commits
5. Create feature branches from `develop`
6. Submit PRs with detailed descriptions

---

**Version**: 2.0.0  
**Last Updated**: 2025-12-31  
**Maintainers**: AI Trade Finder Team
