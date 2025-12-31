# Quick Reference - AI Trade Finder

## 🚀 Common Operations

### Trade Finder Operations (V1)

```bash
# Manual trigger for all symbols
curl -X POST http://localhost:8082/api/v1/trade-finder/trigger

# Get trades for specific symbol
curl http://localhost:8082/api/v1/trade-finder/trades/NQ | jq

# Get recent trades (last 24 hours)
curl http://localhost:8082/api/v1/trade-finder/trades/recent?hours=24 | jq

# Get system statistics
curl http://localhost:8082/api/v1/trade-finder/statistics | jq

# Health check
curl http://localhost:8082/api/v1/trade-finder/health
```

---

## 🤖 AI Operations (V2)

### Structured Trade Analysis

```bash
# Analyze specific trade setup
curl -X POST http://localhost:8082/api/v2/ai/analyze/trade \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "NQ",
    "timeframe": "5m",
    "marketData": {
      "price": 21850,
      "volume": 150000
    },
    "indicators": ["RSI", "MACD", "CISD"]
  }' | jq
```

### Generic Analysis

```bash
# Market structure analysis
curl -X POST http://localhost:8082/api/v2/ai/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "analysisType": "MARKET_STRUCTURE",
    "input": "Analyze NQ from last 2 hours",
    "maxTokens": 3000
  }' | jq
```

---

## 💬 Chat Operations

### Create & Use Chat Session

```bash
# 1. Create new chat session
curl -X POST http://localhost:8082/api/v2/ai/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "title": "NQ Analysis Discussion",
    "type": "GENERAL",
    "symbol": "NQ"
  }' | jq

# Save session ID
SESSION_ID="<copy from response>"

# 2. Send message
curl -X POST http://localhost:8082/api/v2/ai/chat/sessions/$SESSION_ID/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "What are key levels for NQ?"}' | jq

# 3. Get chat history
curl http://localhost:8082/api/v2/ai/chat/sessions/$SESSION_ID | jq

# 4. Close session when done
curl -X POST http://localhost:8082/api/v2/ai/chat/sessions/$SESSION_ID/close
```

### Discuss Specific Trade

```bash
# Get trade ID
TRADE_ID=$(curl -s http://localhost:8082/api/v1/trade-finder/trades/NQ | jq -r '.[0].id')

# Start trade discussion
curl -X POST http://localhost:8082/api/v2/ai/chat/discuss-trade \
  -H "Content-Type: application/json" \
  -d "{
    \"tradeId\": \"$TRADE_ID\",
    \"initialQuestion\": \"Why is this high confidence?\"
  }" | jq

# Get all sessions for this trade
curl http://localhost:8082/api/v2/ai/chat/trades/$TRADE_ID/sessions | jq
```

### List Active Sessions

```bash
# Get all active chat sessions
curl http://localhost:8082/api/v2/ai/chat/sessions/active | jq
```

---

## ⚙️ Dynamic Scheduler Management

### Create Scheduler

```bash
# Create CRON scheduler
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "nq-analyzer-5m",
    "description": "NQ analyzer every 5 minutes",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "0 */5 * * * *",
    "scheduleType": "CRON",
    "parameters": {
      "symbols": ["NQ"],
      "minConfidence": 75
    },
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 10
  }' | jq

# Create FIXED_RATE scheduler (milliseconds)
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "es-analyzer-10m",
    "description": "ES analyzer every 10 minutes",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "600000",
    "scheduleType": "FIXED_RATE",
    "parameters": {
      "symbols": ["ES"]
    },
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 8
  }' | jq
```

### Manage Schedulers

```bash
# List all schedulers
curl http://localhost:8082/api/v2/schedulers | jq

# Get specific scheduler
curl http://localhost:8082/api/v2/schedulers/nq-analyzer-5m | jq

# Get active schedulers
curl http://localhost:8082/api/v2/schedulers/active | jq

# Enable scheduler
curl -X POST http://localhost:8082/api/v2/schedulers/nq-analyzer-5m/enable

# Disable scheduler
curl -X POST http://localhost:8082/api/v2/schedulers/nq-analyzer-5m/disable

# Update scheduler
curl -X PUT http://localhost:8082/api/v2/schedulers/nq-analyzer-5m \
  -H "Content-Type: application/json" \
  -d '{
    "scheduleExpression": "0 */3 * * * *",
    "parameters": {"minConfidence": 80}
  }' | jq

# Delete scheduler
curl -X DELETE http://localhost:8082/api/v2/schedulers/nq-analyzer-5m
```

### Scheduler Statistics

```bash
# Get all scheduler statistics
curl http://localhost:8082/api/v2/schedulers/statistics | jq

# Example output:
# {
#   "totalSchedulers": 5,
#   "enabledSchedulers": 4,
#   "activeSchedulers": 4,
#   "totalExecutions": 1500,
#   "totalFailures": 3
# }
```

---

## 📊 Common Workflows

### Workflow 1: Setup New Symbol Monitoring

```bash
# Create dedicated scheduler for YM
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "ym-finder",
    "description": "YM-specific trade finder",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "0 */10 * * * *",
    "scheduleType": "CRON",
    "parameters": {
      "symbols": ["YM"],
      "timeframes": ["5m", "15m"],
      "minConfidence": 70
    },
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 10
  }'

# Verify it's running
curl http://localhost:8082/api/v2/schedulers/active | jq '.[] | select(.name=="ym-finder")'

# Check for trades after a few minutes
sleep 300
curl http://localhost:8082/api/v1/trade-finder/trades/YM | jq
```

### Workflow 2: Emergency Pause & Resume

```bash
# Emergency: Disable all trade finders
for scheduler in $(curl -s http://localhost:8082/api/v2/schedulers | jq -r '.[] | select(.type=="TRADE_FINDER") | .name'); do
  curl -X POST http://localhost:8082/api/v2/schedulers/$scheduler/disable
  echo "Disabled: $scheduler"
done

# Later: Re-enable all
for scheduler in trade-finder-main trade-finder-secondary; do
  curl -X POST http://localhost:8082/api/v2/schedulers/$scheduler/enable
  echo "Enabled: $scheduler"
done
```

### Workflow 3: Analyze & Discuss Trade

```bash
# Step 1: Get latest trade
TRADE=$(curl -s http://localhost:8082/api/v1/trade-finder/trades/NQ | jq '.[0]')
TRADE_ID=$(echo $TRADE | jq -r '.id')

# Step 2: Review trade details
echo $TRADE | jq '{symbol, direction, confidence, entry, targets}'

# Step 3: Start discussion
SESSION=$(curl -s -X POST http://localhost:8082/api/v2/ai/chat/discuss-trade \
  -H "Content-Type: application/json" \
  -d "{
    \"tradeId\": \"$TRADE_ID\",
    \"initialQuestion\": \"Explain the setup in simple terms\"
  }")

SESSION_ID=$(echo $SESSION | jq -r '.sessionId')

# Step 4: Ask follow-up questions
curl -X POST http://localhost:8082/api/v2/ai/chat/sessions/$SESSION_ID/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "What are the main risks?"}' | jq '.message'

curl -X POST http://localhost:8082/api/v2/ai/chat/sessions/$SESSION_ID/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "Should I wait for confirmation?"}' | jq '.message'
```

### Workflow 4: High-Frequency Monitoring During Events

```bash
# Before FOMC: Create temporary high-frequency scheduler
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "fomc-monitor",
    "description": "High-frequency monitoring during FOMC",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "60000",
    "scheduleType": "FIXED_RATE",
    "parameters": {
      "symbols": ["NQ", "ES"],
      "minConfidence": 80
    },
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 100
  }'

# After event: Delete temporary scheduler
curl -X DELETE http://localhost:8082/api/v2/schedulers/fomc-monitor
```

---

## 🔍 Debugging & Monitoring

### Check System Health

```bash
# Overall health
curl http://localhost:8082/actuator/health | jq

# MongoDB connection
curl http://localhost:8082/actuator/health | jq '.components.mongo'

# Disk space
curl http://localhost:8082/actuator/health | jq '.components.diskSpace'
```

### View Metrics

```bash
# All metrics
curl http://localhost:8082/actuator/metrics | jq

# Specific metric
curl http://localhost:8082/actuator/metrics/jvm.memory.used | jq

# HTTP server metrics
curl http://localhost:8082/actuator/metrics/http.server.requests | jq
```

### Check Scheduled Tasks

```bash
# View all scheduled tasks (including dynamic)
curl http://localhost:8082/actuator/scheduledtasks | jq

# Count active tasks
curl http://localhost:8082/actuator/scheduledtasks | jq '.cron | length'
```

---

## 🗃️ MongoDB Queries

### Direct MongoDB Access

```bash
# Connect to MongoDB
mongosh mongodb://localhost:27017/tradedb

# View recent trades
db.identified_trades.find().sort({created_at: -1}).limit(5).pretty()

# Count trades by symbol
db.identified_trades.aggregate([
  {$group: {_id: "$symbol", count: {$sum: 1}}}
])

# Get active schedulers
db.scheduler_configs.find({enabled: true}).pretty()

# View chat sessions
db.chat_sessions.find({active: true}).pretty()

# Count executions per scheduler
db.scheduler_configs.aggregate([
  {$project: {
    name: 1,
    total_executions: "$execution_stats.total_executions"
  }}
])
```

---

## ⚡ Performance Tips

### Optimize Scheduler Frequency

```bash
# Start conservative, increase gradually
# Bad: Every 1 minute (60 API calls/hour)
"scheduleExpression": "0 * * * * *"

# Good: Every 5 minutes (12 API calls/hour)
"scheduleExpression": "0 */5 * * * *"

# Better for specific events: Every 15 minutes
"scheduleExpression": "0 */15 * * * *"
```

### Monitor OpenAI Usage

```bash
# Track AI calls via statistics
curl http://localhost:8082/api/v1/trade-finder/statistics | jq '{
  total_trades: .totalTrades,
  avg_confidence: .averageConfidence,
  estimated_api_calls: (.totalTrades * 1.5)
}'
```

### Database Maintenance

```bash
# Create indexes (run once)
mongosh mongodb://localhost:27017/tradedb --eval '
  db.identified_trades.createIndex({symbol: 1, created_at: -1});
  db.identified_trades.createIndex({dedupe_key: 1}, {unique: true});
  db.scheduler_configs.createIndex({name: 1}, {unique: true});
  db.chat_sessions.createIndex({trade_id: 1});
'

# Cleanup old trades (>30 days)
mongosh mongodb://localhost:27017/tradedb --eval '
  db.identified_trades.deleteMany({
    created_at: {$lt: new Date(Date.now() - 30*24*60*60*1000)}
  })
'
```

---

## 🐛 Troubleshooting

### Scheduler Not Running

```bash
# Check if enabled
curl http://localhost:8082/api/v2/schedulers/trade-finder-main | jq '.enabled'

# Check active schedulers
curl http://localhost:8082/api/v2/schedulers/active | jq 'length'

# View execution stats
curl http://localhost:8082/api/v2/schedulers/trade-finder-main | jq '.executionStats'

# Check logs
tail -f logs/application.log | grep DynamicSchedulerService
```

### OpenAI API Errors

```bash
# Test API connectivity
curl -X POST http://localhost:8082/api/v2/ai/analyze \
  -H "Content-Type: application/json" \
  -d '{"input": "test", "analysisType": "GENERAL"}'

# Check for rate limits
tail -f logs/application.log | grep -i "rate\|429"

# Verify API key
echo $OPENAI_API_KEY | cut -c1-10
```

### No Trades Generated

```bash
# Check if data sources are available
curl http://localhost:8082/api/v1/trade-finder/health | jq

# Manually trigger
curl -X POST http://localhost:8082/api/v1/trade-finder/trigger

# View statistics
curl http://localhost:8082/api/v1/trade-finder/statistics | jq

# Check MongoDB for raw events
mongosh mongodb://localhost:27017/tradedb --eval '
  db.transformed_events.count()
'
```

---

## 📋 Cheat Sheet

| Operation | Command |
|-----------|---------|
| Create Scheduler | `POST /api/v2/schedulers` |
| Enable Scheduler | `POST /api/v2/schedulers/{name}/enable` |
| Disable Scheduler | `POST /api/v2/schedulers/{name}/disable` |
| Start Chat | `POST /api/v2/ai/chat/sessions` |
| Send Message | `POST /api/v2/ai/chat/sessions/{id}/messages` |
| Get Trades | `GET /api/v1/trade-finder/trades/{symbol}` |
| Health Check | `GET /actuator/health` |
| View Metrics | `GET /actuator/metrics` |

---

**Version**: 2.0  
**Last Updated**: 2025-12-31
