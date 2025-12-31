# AI Trade Finder - Dynamic Trading System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![MongoDB](https://img.shields.io/badge/MongoDB-6.0+-green.svg)](https://www.mongodb.com/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o-blue.svg)](https://openai.com/)

> **AI-powered trading opportunity identification system with dynamic configuration, multi-turn chat capabilities, and OpenAI structured outputs**

---

## 🎯 Features

### Core Capabilities

- **🤖 AI-Powered Trade Identification**: GPT-4o analyzes market structure, Smart Money concepts, and liquidity zones
- **🔄 Dynamic Scheduler Configuration**: Create/modify/delete schedulers via API without deployment
- **💬 Multi-Turn AI Chat**: Discuss trades with context-aware AI assistant
- **📊 Structured Outputs**: Type-safe JSON responses from OpenAI using response API
- **⚡ Real-Time Configuration**: Zero downtime parameter changes
- **🎯 Smart Money Concepts**: CISD, FVG, CHoCH, BOS detection
- **📈 Multi-Timeframe Analysis**: 5m, 15m, 1h concurrent analysis
- **🔔 Alert Integration**: Extensible alert system (webhook, email, SMS)

### Architecture Highlights

- **Stateless REST APIs**: V1 (trade finder) + V2 (unified AI & schedulers)
- **MongoDB Persistence**: Scalable document storage
- **Spring TaskScheduler**: Thread-pooled dynamic scheduling
- **Deduplication Logic**: Prevents duplicate trade alerts
- **Session Management**: Conversation history tracking
- **Lifecycle Management**: Automatic trade expiration

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   REST API Layer (V1 & V2)                  │
├──────────────────┬──────────────────┬──────────────────────┤
│ TradeFinderCtrl  │ UnifiedAICtrl    │ SchedulerConfigCtrl  │
│ (V1: /api/v1/)   │ (V2: /api/v2/ai) │ (V2: /api/v2/sched)  │
└────────┬─────────┴────────┬─────────┴────────┬─────────────┘
         │                  │                   │
         ▼                  ▼                   ▼
┌─────────────────┐  ┌──────────────┐  ┌──────────────────┐
│AITradeFinderSvc │  │AIClientSvc   │  │DynamicScheduler  │
│ (Core Logic)    │  │ (OpenAI API) │  │ Service          │
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

## 🚀 Quick Start

### Prerequisites

- **Java 17+**: [Download](https://adoptium.net/)
- **Maven 3.9+**: [Download](https://maven.apache.org/download.cgi)
- **MongoDB 6.0+**: [Download](https://www.mongodb.com/try/download/community)
- **OpenAI API Key**: [Get Key](https://platform.openai.com/api-keys)

### Installation

```bash
# Clone repository
git clone https://github.com/your-org/ai-trade-finder.git
cd ai-trade-finder

# Set environment variables
export OPENAI_API_KEY="your-openai-api-key"
export MONGODB_URI="mongodb://localhost:27017/tradedb"

# Build
mvn clean install

# Run
mvn spring-boot:run
```

### Verify Installation

```bash
# Health check
curl http://localhost:8082/actuator/health

# Expected output:
# {"status":"UP"}
```

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [API V2 Documentation](docs/API_V2_DOCUMENTATION.md) | Complete REST API reference |
| [Implementation Guide](docs/IMPLEMENTATION_GUIDE.md) | Architecture patterns & deployment |
| [Quick Reference](docs/QUICK_REFERENCE.md) | Common operations cheat sheet |
| [AI Workflow Examples](docs/AI_WORKFLOW_EXAMPLES.md) | Prompt engineering guide |

---

## 🎮 Usage Examples

### Example 1: Create Custom Scheduler

```bash
# Create scheduler for ES symbol every 10 minutes
curl -X POST http://localhost:8082/api/v2/schedulers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "es-finder-10m",
    "description": "ES trade finder every 10 minutes",
    "type": "TRADE_FINDER",
    "enabled": true,
    "scheduleExpression": "0 */10 * * * *",
    "scheduleType": "CRON",
    "parameters": {
      "symbols": ["ES"],
      "minConfidence": 75
    },
    "handlerBean": "aiTradeFinderService",
    "handlerMethod": "findTrades",
    "priority": 10
  }'
```

### Example 2: Chat About a Trade

```bash
# Get recent trade
TRADE_ID=$(curl -s http://localhost:8082/api/v1/trade-finder/trades/NQ | jq -r '.[0].id')

# Start discussion
curl -X POST http://localhost:8082/api/v2/ai/chat/discuss-trade \
  -H "Content-Type: application/json" \
  -d "{
    \"tradeId\": \"$TRADE_ID\",
    \"initialQuestion\": \"What's the risk-reward ratio?\"
  }"
```

### Example 3: Structured Analysis

```bash
# Analyze market structure with structured output
curl -X POST http://localhost:8082/api/v2/ai/analyze/trade \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "NQ",
    "timeframe": "5m",
    "marketData": {"price": 21850, "volume": 150000}
  }' | jq
```

---

## 🔧 Configuration

### application.properties

```properties
# Server
server.port=8082

# MongoDB
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/tradedb}

# OpenAI
openai.api.key=${OPENAI_API_KEY}
openai.api.model=gpt-4o
openai.api.max-tokens=4000
openai.api.temperature=0.7

# Dynamic Scheduler
scheduler.pool-size=10
scheduler.thread-name-prefix=dynamic-scheduler-

# Chat
chat.max-history-messages=50
chat.default-model=gpt-4o

# Trade Finder
trade.finder.default.symbols=NQ,ES,YM,RTY
trade.finder.min.confidence=70
trade.finder.expiration.hours=8
```

---

## 🗄️ Database Schema

### Collections

| Collection | Purpose | Key Fields |
|------------|---------|------------|
| `identified_trades` | AI-identified trading opportunities | symbol, direction, confidence, entry, targets |
| `scheduler_configs` | Dynamic scheduler definitions | name, scheduleExpression, handlerBean |
| `chat_sessions` | Multi-turn conversations | tradeId, messages[], context |
| `transformed_events` | Market event processing | symbol, eventType, timestamp |
| `ohlc_data` | OHLC price data | symbol, timeframe, open, high, low, close |

### Indexes

```javascript
// Critical indexes for performance
db.identified_trades.createIndex({ symbol: 1, created_at: -1 })
db.identified_trades.createIndex({ dedupe_key: 1 }, { unique: true })
db.scheduler_configs.createIndex({ name: 1 }, { unique: true })
db.chat_sessions.createIndex({ trade_id: 1 })
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UnifiedAIControllerTest

# Integration tests
mvn verify
```

---

## 📊 API Endpoints

### Trade Finder API (V1)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/trade-finder/trigger` | Manual trade search |
| GET | `/api/v1/trade-finder/trades/{symbol}` | Get trades by symbol |
| GET | `/api/v1/trade-finder/trades/recent` | Recent trades (24h) |
| GET | `/api/v1/trade-finder/statistics` | System statistics |

### Unified AI API (V2)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/ai/analyze/trade` | Structured trade analysis |
| POST | `/api/v2/ai/analyze` | Generic analysis |
| POST | `/api/v2/ai/chat/sessions` | Create chat session |
| POST | `/api/v2/ai/chat/sessions/{id}/messages` | Send message |
| POST | `/api/v2/ai/chat/discuss-trade` | Discuss specific trade |

### Scheduler Management (V2)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/schedulers` | Create scheduler |
| GET | `/api/v2/schedulers` | List all schedulers |
| PUT | `/api/v2/schedulers/{name}` | Update scheduler |
| DELETE | `/api/v2/schedulers/{name}` | Delete scheduler |
| POST | `/api/v2/schedulers/{name}/enable` | Enable scheduler |
| GET | `/api/v2/schedulers/statistics` | Scheduler stats |

---

## 🛠️ Development

### Project Structure

```
src/main/java/com/trade/app/
├── alert/              # Alert providers (webhook, email, etc.)
├── config/             # Spring configuration classes
├── controller/         # REST API controllers
├── datasource/         # External data source clients
├── decision/           # Trade decision logic
├── domain/             # Domain models & DTOs
├── openai/             # OpenAI integration
├── orchestrator/       # Workflow orchestration
├── payload/            # Payload builders
├── persistence/        # MongoDB entities & repositories
├── scheduler/          # Dynamic scheduler service
└── util/               # Utility classes

src/main/resources/
├── prompts/            # System prompts for AI
├── application.properties
└── static/

src/test/               # Unit & integration tests
```

### Code Style

- Follow Spring Boot best practices
- Use Lombok for boilerplate reduction
- Write comprehensive JavaDocs
- Maintain 80%+ test coverage
- Use conventional commits

---

## 🚀 Deployment

### Docker Deployment

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/ai-trade-finder-*.jar app.jar
ENV JAVA_OPTS="-Xmx512m -Xms256m"
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

```bash
# Build image
docker build -t ai-trade-finder:latest .

# Run container
docker run -d \
  --name trade-finder \
  -p 8082:8082 \
  -e OPENAI_API_KEY=your-key \
  -e MONGODB_URI=mongodb://host:27017/tradedb \
  ai-trade-finder:latest
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-trade-finder
spec:
  replicas: 2
  selector:
    matchLabels:
      app: trade-finder
  template:
    metadata:
      labels:
        app: trade-finder
    spec:
      containers:
      - name: trade-finder
        image: ai-trade-finder:latest
        ports:
        - containerPort: 8082
        env:
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: openai-secret
              key: api-key
        - name: MONGODB_URI
          value: "mongodb://mongo-service:27017/tradedb"
```

---

## 📈 Monitoring

### Actuator Endpoints

```bash
# Health check
curl http://localhost:8082/actuator/health

# Metrics
curl http://localhost:8082/actuator/metrics

# Scheduled tasks
curl http://localhost:8082/actuator/scheduledtasks
```

### Prometheus Integration

Add to `application.properties`:
```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

---

## 📝 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **OpenAI**: GPT-4o API for AI analysis
- **Spring Framework**: Robust application framework
- **MongoDB**: Flexible document storage
- **Smart Money Concepts**: Trading methodology

---

## 📞 Support

- **Documentation**: [API_V2_DOCUMENTATION.md](docs/API_V2_DOCUMENTATION.md)
- **Issues**: [GitHub Issues](https://github.com/your-org/ai-trade-finder/issues)
- **Email**: support@aitradefinder.com

---

## 🔮 Roadmap

- [ ] WebSocket support for real-time alerts
- [ ] Multi-model AI support (Claude, Llama)
- [ ] Advanced backtesting engine
- [ ] Mobile app (React Native)
- [ ] Performance analytics dashboard
- [ ] Multi-user authentication (JWT)
- [ ] Telegram/Discord bot integration

---

**Built with ❤️ by AI Trade Finder Team**  
**Version**: 2.0.0  
**Last Updated**: 2025-12-31
