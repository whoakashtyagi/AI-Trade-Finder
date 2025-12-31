# Configuration Guide - AI Trade Finder

## Overview

This guide explains how to properly configure the AI Trade Finder application for different environments (development, production, testing).

## Security Best Practices

🔒 **CRITICAL**: Never commit sensitive credentials to version control!

- All credentials should be provided via environment variables
- Use `.env` files for local development (ensure `.env` is in `.gitignore`)
- Use secure secret management in production (AWS Secrets Manager, Azure Key Vault, etc.)

## Environment Profiles

The application supports multiple Spring profiles:

- `dev` - Development environment (default)
- `prod` - Production environment
- `test` - Testing environment

### Activating a Profile

**Via environment variable:**
```bash
export SPRING_PROFILES_ACTIVE=prod
```

**Via application startup:**
```bash
java -jar app.jar --spring.profiles.active=prod
```

**Via application.properties:**
```properties
spring.profiles.active=prod
```

## Required Environment Variables

### MongoDB Configuration

```bash
# MongoDB connection string
MONGO_URI=mongodb://username:password@host:port/database
```

**Example (local development):**
```bash
MONGO_URI=mongodb://localhost:27017/trade_app_dev
```

**Example (production with authentication):**
```bash
MONGO_URI=mongodb://user:password@prod-mongo.example.com:27017/trade_app_prod?authSource=admin
```

### OpenAI Configuration

```bash
# Required: Your OpenAI API key
OPENAI_API_KEY=sk-proj-your-api-key-here

# Optional: Custom OpenAI base URL (default: https://api.openai.com/v1)
OPENAI_BASE_URL=https://api.openai.com/v1

# Optional: Default model (default: gpt-4o)
OPENAI_DEFAULT_MODEL=gpt-4o

# Optional: Request timeout in seconds (default: 60)
OPENAI_TIMEOUT_SECONDS=60

# Optional: Maximum retry attempts (default: 3)
OPENAI_MAX_RETRIES=3

# Optional: Enable request/response logging (default: false)
OPENAI_LOGGING_ENABLED=false
```

## Application Configuration

### AI Trade Finder Settings

```bash
# Enable/disable the scheduled trade finder job
AI_TRADE_FINDER_ENABLED=true

# Symbols to analyze (comma-separated)
AI_TRADE_FINDER_SYMBOLS=NQ,ES,YM,GC,RTY

# Scheduler interval in milliseconds (300000 = 5 minutes)
AI_TRADE_FINDER_INTERVAL_MS=300000

# How many minutes back to look for market events
AI_TRADE_FINDER_EVENT_LOOKBACK_MINUTES=90

# Number of OHLC candles to fetch per timeframe
AI_TRADE_FINDER_OHLC_CANDLE_COUNT=100

# Trade expiry time in hours
AI_TRADE_FINDER_TRADE_EXPIRY_HOURS=4

# Analysis profile to use
AI_TRADE_FINDER_ANALYSIS_PROFILE=SILVER_BULLET_WINDOW

# Confidence thresholds
AI_TRADE_FINDER_CONFIDENCE_HIGH=80
AI_TRADE_FINDER_CONFIDENCE_MEDIUM=60
```

### Scheduler Settings

```bash
# Thread pool size for dynamic schedulers
SCHEDULER_POOL_SIZE=10
```

### Chat Settings

```bash
# Maximum number of messages to keep in chat history
CHAT_MAX_HISTORY_MESSAGES=50

# Default model for chat operations
CHAT_DEFAULT_MODEL=gpt-4o

# Maximum tokens for chat responses
CHAT_MAX_TOKENS=2000
```

## Setup Instructions

### 1. Local Development Setup

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` and fill in your credentials:**
   ```bash
   nano .env
   # or
   code .env
   ```

3. **Ensure MongoDB is running locally:**
   ```bash
   # Using Docker
   docker run -d -p 27017:27017 --name mongodb mongo:latest
   ```

4. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

### 2. Production Deployment

#### Option A: Environment Variables (Recommended)

Set environment variables directly in your deployment platform:

**Docker:**
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
ENV MONGO_URI=mongodb://...
ENV OPENAI_API_KEY=sk-proj-...
```

**Kubernetes:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  MONGO_URI: base64-encoded-value
  OPENAI_API_KEY: base64-encoded-value
```

**AWS ECS/Fargate:**
Use AWS Secrets Manager and reference secrets in task definition.

**Heroku/Railway:**
Set config vars in the platform dashboard.

#### Option B: application-prod.properties

Ensure `src/main/resources/application-prod.properties` uses environment variable placeholders:
```properties
spring.mongodb.uri=${MONGO_URI}
openai.api-key=${OPENAI_API_KEY}
```

Then set the environment variables in your deployment environment.

### 3. Testing Setup

For integration tests, use `application-test.properties` with test-specific settings:

```properties
spring.mongodb.uri=mongodb://localhost:27017/trade_app_test
openai.api-key=${OPENAI_API_KEY:test-key}
```

## Configuration Validation

The application will fail to start if required configuration is missing:

- ✅ **Missing OpenAI API Key**: `IllegalStateException: OpenAI API key is not configured`
- ✅ **Invalid MongoDB URI**: Connection error on startup

Check logs for configuration errors:
```bash
tail -f logs/application.log | grep -i "error\|configuration"
```

## Configuration Files Reference

| File | Purpose |
|------|---------|
| `application.properties` | Main configuration with sensible defaults |
| `application-dev.properties` | Development-specific settings |
| `application-prod.properties` | Production-specific settings |
| `application-test.properties` | Test-specific settings |
| `.env.example` | Template for local environment variables |
| `.env` | **Local only** - Your actual credentials (gitignored) |

## Troubleshooting

### Issue: Application won't start - Missing API key

**Error:**
```
Caused by: java.lang.IllegalStateException: OpenAI API key is not configured
```

**Solution:**
Ensure `OPENAI_API_KEY` environment variable is set:
```bash
export OPENAI_API_KEY=sk-proj-your-key-here
```

### Issue: MongoDB connection failed

**Error:**
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms
```

**Solution:**
1. Verify MongoDB is running: `mongosh --eval "db.serverStatus()"`
2. Check `MONGO_URI` is correct
3. Ensure network connectivity and firewall rules

### Issue: Profile not loaded

**Symptom:** Configuration from profile-specific file not applied

**Solution:**
Verify active profile:
```bash
# Check logs for:
# "The following profiles are active: dev"
```

Set explicitly if needed:
```bash
export SPRING_PROFILES_ACTIVE=prod
```

## Security Checklist

- [ ] `.env` is in `.gitignore`
- [ ] No hardcoded credentials in source code
- [ ] Production uses environment variables
- [ ] API keys rotate regularly
- [ ] MongoDB uses authentication in production
- [ ] HTTPS/TLS enabled for external connections
- [ ] Secrets stored in secure vault (production)

## Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [OpenAI API Documentation](https://platform.openai.com/docs)
- [MongoDB Connection Strings](https://www.mongodb.com/docs/manual/reference/connection-string/)

---

**Last Updated:** December 2025
