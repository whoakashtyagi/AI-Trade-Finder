# AI Trade Finder - Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### 1. Verify Configuration
Check `application.properties`:
```properties
ai.trade-finder.enabled=true
ai.trade-finder.symbols=NQ,ES,YM,GC,RTY
openai.api-key=your-api-key-here
spring.mongodb.uri=your-mongodb-uri
```

### 2. Start the Application
```bash
mvn spring-boot:run
```

### 3. Verify Service is Running
```bash
curl http://localhost:8082/api/v1/trade-finder/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "AI Trade Finder",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 4. Manual Trigger (Optional)
```bash
curl -X POST http://localhost:8082/api/v1/trade-finder/trigger
```

### 5. Check for Identified Trades
```bash
curl http://localhost:8082/api/v1/trade-finder/trades/NQ
```

---

## 📋 Quick Reference

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/trade-finder/trigger` | Manually run trade finder |
| GET | `/api/v1/trade-finder/trades/{symbol}` | Get trades for symbol |
| GET | `/api/v1/trade-finder/trades/recent?hours=24` | Recent trades |
| GET | `/api/v1/trade-finder/statistics` | Trade statistics |
| GET | `/api/v1/trade-finder/health` | Health check |

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Enable/disable scheduler |
| `symbols` | `NQ,ES,YM,GC,RTY` | Symbols to analyze |
| `interval-ms` | `300000` | Run every 5 minutes |
| `event-lookback-minutes` | `90` | Event history window |
| `confidence-threshold-high` | `80` | High confidence (≥80%) |
| `confidence-threshold-medium` | `60` | Medium confidence (≥60%) |

### Confidence Levels

| Confidence | Alert Type | Example |
|------------|------------|---------|
| ≥ 80% | CALL + SMS + Telegram | Perfect setup |
| 60-79% | SMS + Telegram | Good setup |
| < 60% | Log only | Weak setup |

### Trade Statuses

| Status | Description |
|--------|-------------|
| `IDENTIFIED` | Active trade opportunity |
| `ALERTED` | User notified |
| `EXPIRED` | Setup expired (4 hours) |
| `TAKEN` | User took the trade |
| `INVALIDATED` | Setup no longer valid |

---

## 🔧 Common Commands

### Check Recent Trades
```bash
curl http://localhost:8082/api/v1/trade-finder/trades/recent?hours=24 | jq
```

### Get Statistics
```bash
curl http://localhost:8082/api/v1/trade-finder/statistics | jq
```

### Filter by Status
```bash
curl http://localhost:8082/api/v1/trade-finder/trades/NQ/status/IDENTIFIED | jq
```

### Manual Trigger with Timing
```bash
time curl -X POST http://localhost:8082/api/v1/trade-finder/trigger
```

---

## 🐛 Troubleshooting

### No Trades Found
**Check:**
1. Events exist: `db.transformed_events.count()`
2. OHLC data exists: `db.ohlc_data.count()`
3. Symbols configured correctly
4. AI API key is valid

### Scheduler Not Running
**Check:**
1. `@EnableScheduling` in main application class
2. `ai.trade-finder.enabled=true` in properties
3. No startup errors in logs

### AI Errors
**Check:**
1. OpenAI API key validity
2. Network connectivity
3. Request payload size
4. API rate limits

### Duplicate Trades
**Check:**
1. Unique index on `dedupe_key`: 
   ```javascript
   db.identified_trades.getIndexes()
   ```
2. Deduplication logic in service

---

## 📊 Monitoring

### Key Log Messages
```
[INFO] === AI Trade Finder: Starting scheduled run ===
[INFO] Analyzing symbol: NQ
[INFO] Found 25 transformed events for NQ
[INFO] Saved identified trade: NQ LONG at 21780-21800 with confidence 85
[INFO] HIGH CONFIDENCE TRADE: NQ LONG with 85% confidence
[INFO] === AI Trade Finder: Completed in 2345ms. Trades found: 3 ===
```

### Watch Logs
```bash
tail -f logs/application.log | grep "AI Trade Finder"
```

### Statistics Schedule
- Trade expiration check: Every 15 minutes
- Statistics logging: Every hour

---

## 🗄️ MongoDB Queries

### Check Identified Trades
```javascript
db.identified_trades.find().sort({identified_at: -1}).limit(10)
```

### Count Active Trades
```javascript
db.identified_trades.count({status: "IDENTIFIED"})
```

### Find High Confidence Trades
```javascript
db.identified_trades.find({confidence: {$gte: 80}}).sort({identified_at: -1})
```

### Check Duplicates
```javascript
db.identified_trades.aggregate([
  {$group: {_id: "$dedupe_key", count: {$sum: 1}}},
  {$match: {count: {$gt: 1}}}
])
```

---

## 🎯 Testing Checklist

- [ ] Service starts without errors
- [ ] Health endpoint responds
- [ ] Manual trigger works
- [ ] Trades are saved to MongoDB
- [ ] Deduplication prevents duplicates
- [ ] Statistics endpoint shows data
- [ ] Logs show trade identifications
- [ ] Trade expiration works (after 4 hours)

---

## 📖 Documentation Links

- **Full Documentation**: [AI_TRADE_FINDER_README.md](AI_TRADE_FINDER_README.md)
- **Implementation Summary**: [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- **File Structure**: [AI_TRADE_FINDER_FILE_STRUCTURE.md](AI_TRADE_FINDER_FILE_STRUCTURE.md)

---

## 🔐 Security Notes

1. **API Key**: Never commit OpenAI API key to Git
2. **MongoDB URI**: Use environment variables for production
3. **Rate Limiting**: Consider adding rate limits to manual trigger endpoint
4. **Authentication**: Add auth to REST endpoints in production

---

## 🚢 Deployment

### Environment Variables
```bash
export AI_TRADE_FINDER_ENABLED=true
export AI_TRADE_FINDER_SYMBOLS=NQ,ES,YM,GC,RTY
export OPENAI_API_KEY=your-key
export SPRING_MONGODB_URI=your-uri
```

### Docker (Future)
```bash
docker build -t ai-trade-finder .
docker run -p 8082:8082 \
  -e AI_TRADE_FINDER_ENABLED=true \
  -e OPENAI_API_KEY=your-key \
  ai-trade-finder
```

---

## 💡 Tips & Best Practices

1. **Start Small**: Test with 1-2 symbols first
2. **Monitor Costs**: OpenAI API calls cost money
3. **Tune Thresholds**: Adjust confidence thresholds based on results
4. **Log Analysis**: Review logs regularly for patterns
5. **Backup Data**: Export identified trades periodically
6. **Alert Testing**: Test alert dispatch separately
7. **Performance**: Monitor AI response times
8. **Data Quality**: Ensure transformed_events are accurate

---

## 📞 Support

For issues or questions:
1. Check logs: `logs/application.log`
2. Review documentation: `AI_TRADE_FINDER_README.md`
3. Verify configuration: `application.properties`
4. Test manually: Use REST API endpoints
5. Check MongoDB: Verify data exists

---

**Quick Start Guide Version**: 1.0  
**Last Updated**: 2025-01-15
