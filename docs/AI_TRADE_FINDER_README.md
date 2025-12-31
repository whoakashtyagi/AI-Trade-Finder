# AI Trade Finder Service

## Overview

The AI Trade Finder Service is an intelligent trading system that automatically analyzes market events, OHLC data, and session context to identify high-confidence trading opportunities using AI-powered analysis based on Smart Money Concepts (SMC) and ICT methodologies.

## Architecture

### Components

1. **AITradeFinderService** (`com.trade.app.decision.AITradeFinderService`)
   - Core service orchestrating the trade finding logic
   - Runs on a configurable schedule (default: every 5 minutes)
   - Queries events, OHLC data, and builds AI payloads
   - Parses AI responses and saves identified trades

2. **IdentifiedTrade Entity** (`com.trade.app.persistence.mongo.document.IdentifiedTrade`)
   - MongoDB document storing identified trading opportunities
   - Includes deduplication mechanism
   - Tracks alert status and trade lifecycle

3. **TradeLifecycleScheduler** (`com.trade.app.scheduler.TradeLifecycleScheduler`)
   - Manages trade expiration
   - Provides periodic statistics
   - Cleanup of stale data

4. **TradeFinderController** (`com.trade.app.controller.TradeFinderController`)
   - REST API for manual triggering and querying trades
   - Statistics and health check endpoints

## Data Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                  AI Trade Finder Service                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┼─────────────┐
                │             │             │
                ▼             ▼             ▼
    ┌───────────────┐ ┌─────────────┐ ┌─────────────┐
    │ Transformed   │ │ OHLC Data   │ │ Daily       │
    │ Events        │ │ Repository  │ │ Trading     │
    │ Repository    │ │             │ │ Data        │
    └───────────────┘ └─────────────┘ └─────────────┘
                │
                ├──── Build Payload
                │
                ▼
    ┌───────────────────────────┐
    │  AI Client Service        │
    │  (OpenAI GPT-4)          │
    └───────────────────────────┘
                │
                ├──── Parse Response
                │
                ▼
    ┌───────────────────────────┐
    │  Deduplication Check      │
    └───────────────────────────┘
                │
                ├──── Save Trade
                │
                ▼
    ┌───────────────────────────┐
    │  Identified Trades        │
    │  (MongoDB Collection)     │
    └───────────────────────────┘
                │
                ├──── Dispatch Alerts
                │
                ▼
    ┌───────────────────────────┐
    │  Alert Service            │
    │  (SMS, Call, Telegram)    │
    └───────────────────────────┘
```

## Configuration

### Application Properties

Add the following to your `application.properties`:

```properties
# AI Trade Finder Configuration
ai.trade-finder.enabled=true
ai.trade-finder.symbols=NQ,ES,YM,GC,RTY
ai.trade-finder.interval-ms=300000
ai.trade-finder.event-lookback-minutes=90
ai.trade-finder.ohlc-candle-count=100
ai.trade-finder.trade-expiry-hours=4
ai.trade-finder.system-prompt-file=prompts/trade_finder_system.txt
ai.trade-finder.analysis-profile=SILVER_BULLET_WINDOW
ai.trade-finder.confidence-threshold-high=80
ai.trade-finder.confidence-threshold-medium=60
```

### Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Enable/disable the scheduler |
| `symbols` | `NQ,ES,YM,GC,RTY` | Symbols to analyze |
| `interval-ms` | `300000` | Scheduler interval (5 minutes) |
| `event-lookback-minutes` | `90` | How far back to look for events |
| `ohlc-candle-count` | `100` | Number of candles per timeframe |
| `trade-expiry-hours` | `4` | Hours until trade setup expires |
| `confidence-threshold-high` | `80` | High confidence threshold (≥80%) |
| `confidence-threshold-medium` | `60` | Medium confidence threshold (≥60%) |

## Trading Logic

### Primary Entry Pattern: Sweep → CISD → FVG

1. **Liquidity Sweep**: Price sweeps stops above/below key levels
2. **CISD**: Strong displacement in opposite direction (BOS/CHoCH)
3. **SMT** (optional): Smart Money divergence for confirmation
4. **FVG Entry**: Price retraces to Fair Value Gap for optimal entry

### Confidence Levels

- **≥80%**: High confidence → CALL + SMS + Telegram alerts
- **60-79%**: Medium confidence → SMS + Telegram alerts
- **<60%**: Low confidence → Log only, no alerts

## API Endpoints

### Manual Trigger

```bash
POST /api/v1/trade-finder/trigger
```

Manually triggers the trade finder for all symbols.

**Response:**
```json
{
  "status": "success",
  "message": "Trade finder execution completed",
  "duration_ms": 2345,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### Get Trades by Symbol

```bash
GET /api/v1/trade-finder/trades/{symbol}
```

Retrieves all identified trades for a symbol.

**Example:**
```bash
curl http://localhost:8082/api/v1/trade-finder/trades/NQ
```

### Get Recent Trades

```bash
GET /api/v1/trade-finder/trades/recent?hours=24
```

Retrieves recent trades across all symbols.

### Get Statistics

```bash
GET /api/v1/trade-finder/statistics
```

Returns trade statistics for the last 24 hours.

**Response:**
```json
{
  "period_hours": 24,
  "total_trades": 15,
  "active_trades": 5,
  "expired_trades": 8,
  "alerted_trades": 12,
  "average_confidence": 72.3,
  "by_symbol": {
    "NQ": 8,
    "ES": 5,
    "YM": 2
  },
  "by_direction": {
    "LONG": 9,
    "SHORT": 6
  }
}
```

### Health Check

```bash
GET /api/v1/trade-finder/health
```

## Database Schema

### Collection: `identified_trades`

```javascript
{
  "_id": "...",
  "symbol": "NQ",
  "direction": "LONG",
  "identified_at": ISODate("2025-01-15T10:30:00Z"),
  "confidence": 85,
  "status": "IDENTIFIED",
  "entry_zone_type": "FVG_CE",
  "entry_zone": "21780-21800",
  "entry_price": 21790.0,
  "stop_placement": "Below 21750",
  "targets": ["POC", "Asia High"],
  "rr_hint": "2.5:1",
  "narrative": "NQ swept SSL at 21750, followed by bullish CISD...",
  "trigger_conditions": ["Price reaches FVG CE zone", "..."],
  "invalidations": ["Price breaks below 21750", "..."],
  "session_label": "NY_AM",
  "timeframe": "5m",
  "dedupe_key": "NQ_LONG_21780_21800_20250115_10",
  "alert_sent": true,
  "alert_sent_at": ISODate("2025-01-15T10:30:05Z"),
  "alert_type": "CALL_SMS_TELEGRAM",
  "created_at": ISODate("2025-01-15T10:30:00Z"),
  "expires_at": ISODate("2025-01-15T14:30:00Z")
}
```

### Indexes

- `symbol` (indexed)
- `direction` (indexed)
- `identified_at` (indexed)
- `status` (indexed)
- `dedupe_key` (unique index)

## Deduplication Strategy

Deduplication key format: `{symbol}_{direction}_{entryZone}_{YYYYMMDD_HH}`

Example: `NQ_LONG_21780_21800_20250115_10`

This prevents duplicate alerts for the same trade setup within a 1-hour window.

## Trade Lifecycle

1. **IDENTIFIED**: Trade setup identified by AI
2. **ALERTED**: Alert sent to user(s)
3. **EXPIRED**: Trade setup expired (after 4 hours by default)
4. **TAKEN**: User manually marks trade as taken
5. **INVALIDATED**: Setup invalidated by market action

## Monitoring & Logging

### Log Levels

- **INFO**: Scheduler runs, trade identifications, alerts
- **DEBUG**: Payload building, query details, AI responses
- **ERROR**: AI client errors, database errors, parsing failures

### Key Log Messages

```
[INFO] === AI Trade Finder: Starting scheduled run ===
[INFO] Analyzing symbol: NQ
[INFO] Saved identified trade: NQ LONG at 21780-21800 with confidence 85
[INFO] HIGH CONFIDENCE TRADE: NQ LONG with 85% confidence - Dispatching all alerts
[INFO] === AI Trade Finder: Completed in 2345ms. Trades found: 3 ===
```

## Testing

### Manual Testing

1. Start the application
2. Trigger manually via API:
   ```bash
   curl -X POST http://localhost:8082/api/v1/trade-finder/trigger
   ```

3. Check identified trades:
   ```bash
   curl http://localhost:8082/api/v1/trade-finder/trades/NQ
   ```

4. View statistics:
   ```bash
   curl http://localhost:8082/api/v1/trade-finder/statistics
   ```

### Testing with Mock Data

Ensure your MongoDB has:
- `transformed_events` collection with recent events
- `ohlc_data` collection with OHLC candles
- Valid OpenAI API key configured

## Troubleshooting

### Service Not Running

**Issue**: Scheduler doesn't execute
**Solution**: 
- Check `ai.trade-finder.enabled=true` in properties
- Verify `@EnableScheduling` is present in main application class
- Check logs for exceptions during startup

### No Trades Identified

**Issue**: Service runs but finds no trades
**Solution**:
- Verify transformed events exist in MongoDB
- Check event lookback window (`event-lookback-minutes`)
- Review AI confidence thresholds
- Check system prompt is loading correctly

### AI Errors

**Issue**: AI client exceptions
**Solution**:
- Verify OpenAI API key is valid
- Check network connectivity to OpenAI API
- Review request payload size (might be too large)
- Check OpenAI API rate limits

### Duplicate Trades

**Issue**: Same trade alerted multiple times
**Solution**:
- Verify unique index on `dedupe_key` field
- Check deduplication logic in service
- Review dedupe key generation

## Future Enhancements

1. **Alert Integration**
   - Twilio integration for SMS/calls
   - Telegram bot integration
   - Email notifications

2. **Additional Data Sources**
   - Google Sheets integration for manual key levels
   - DailyTradingData service integration
   - Volume profile data

3. **Advanced Features**
   - Trade execution integration
   - Position sizing calculator
   - Risk management rules
   - Trade journaling

4. **Machine Learning**
   - Trade performance tracking
   - Pattern success rate analysis
   - Adaptive confidence scoring

## Support & Maintenance

### Key Files

- Service: `AITradeFinderService.java`
- Entity: `IdentifiedTrade.java`
- Repository: `IdentifiedTradeRepository.java`
- Controller: `TradeFinderController.java`
- Scheduler: `TradeLifecycleScheduler.java`
- Config: `application.properties`
- Prompt: `resources/prompts/trade_finder_system.txt`

### Dependencies

- Spring Boot 3.x
- Spring Data MongoDB
- OpenAI API Client
- Jackson (JSON processing)
- Lombok (code generation)

---

**Version**: 1.0  
**Last Updated**: 2025-01-15  
**Author**: AI Trade Finder Team
