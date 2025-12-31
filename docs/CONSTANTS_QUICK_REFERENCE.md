# String Constants Quick Reference Guide

## Purpose
This guide helps developers quickly find and use the appropriate constants instead of hardcoded strings.

## How to Use Constants

### Import Statement
Always add this import when using constants:
```java
import com.trade.app.util.Constants;
```

## Available Constants

### 1. Trade Status (`Constants.TradeStatus`)
Used for tracking the lifecycle of identified trades.

```java
Constants.TradeStatus.IDENTIFIED    // Trade has been identified
Constants.TradeStatus.ALERTED       // Alert has been sent
Constants.TradeStatus.EXPIRED       // Trade setup expired
Constants.TradeStatus.CANCELLED     // Trade was cancelled
Constants.TradeStatus.TAKEN         // Trade was entered
Constants.TradeStatus.INVALIDATED   // Trade setup invalidated
```

**Usage Example:**
```java
trade.setStatus(Constants.TradeStatus.IDENTIFIED);
if (Constants.TradeStatus.EXPIRED.equals(trade.getStatus())) {
    // Handle expired trade
}
```

### 2. Trade Direction (`Constants.TradeDirection`)
```java
Constants.TradeDirection.LONG    // Long/buy position
Constants.TradeDirection.SHORT   // Short/sell position
```

### 3. AI Response Status (`Constants.AIResponseStatus`)
```java
Constants.AIResponseStatus.COMPLETED     // AI response completed successfully
Constants.AIResponseStatus.FAILED        // AI request failed
Constants.AIResponseStatus.IN_PROGRESS   // AI processing in progress
Constants.AIResponseStatus.CANCELLED     // AI request was cancelled
```

**Usage Example:**
```java
if (Constants.AIResponseStatus.COMPLETED.equalsIgnoreCase(response.getStatus())) {
    // Process successful response
}
```

### 4. Trade Signal Status (`Constants.TradeSignalStatus`)
```java
Constants.TradeSignalStatus.TRADE_IDENTIFIED   // AI identified a trade
Constants.TradeSignalStatus.NO_SETUP           // No valid setup found
Constants.TradeSignalStatus.INSUFFICIENT_DATA  // Not enough data
Constants.TradeSignalStatus.ERROR              // Error in analysis
```

### 5. Conversation Status (`Constants.ConversationStatus`)
```java
Constants.ConversationStatus.ACTIVE     // Conversation is active
Constants.ConversationStatus.COMPLETED  // Conversation completed
Constants.ConversationStatus.EXPIRED    // Conversation expired
```

**Usage Example:**
```java
conversation.setStatus(Constants.ConversationStatus.ACTIVE);
List<AIConversation> active = repository.findByStatus(
    Constants.ConversationStatus.ACTIVE
);
```

### 6. Conversation Type (`Constants.ConversationType`)
```java
Constants.ConversationType.TRADE_FOLLOWUP    // Follow-up on a trade
Constants.ConversationType.TRADE_ANALYSIS    // Trade analysis
Constants.ConversationType.MARKET_ANALYSIS   // Market analysis
Constants.ConversationType.WORKFLOW          // Workflow conversation
```

### 7. Entity Type (`Constants.EntityType`)
```java
Constants.EntityType.TRADE    // Entity is a trade
Constants.EntityType.SYMBOL   // Entity is a symbol
Constants.EntityType.PATTERN  // Entity is a pattern
```

### 8. Entry Zone Type (`Constants.EntryZoneType`)
```java
Constants.EntryZoneType.FVG_CE      // Fair Value Gap CE
Constants.EntryZoneType.IFVG        // Inverted FVG
Constants.EntryZoneType.OB          // Order Block
Constants.EntryZoneType.BREAKER     // Breaker Block
Constants.EntryZoneType.MITIGATION  // Mitigation Block
```

**Usage Example:**
```java
if (Constants.EntryZoneType.FVG_CE.equals(trade.getEntryZoneType())) {
    description = "Fair Value Gap at Consequent Encroachment";
}
```

### 9. Prompt Type (`Constants.PromptType`)
```java
Constants.PromptType.PREDEFINED  // Use predefined prompt
Constants.PromptType.CUSTOM      // Use custom prompt
```

### 10. Prompt Keys (`Constants.PromptKey`)
```java
Constants.PromptKey.DAY_ANALYSIS       // day_analysis
Constants.PromptKey.SWING_TRADE        // swing_trade
Constants.PromptKey.RISK_ASSESSMENT    // risk_assessment
Constants.PromptKey.MARKET_STRUCTURE   // market_structure
Constants.PromptKey.ENTRY_EXIT         // entry_exit
Constants.PromptKey.GENERAL_ANALYZER   // general_analyzer
```

**Usage Example:**
```java
Map<String, String> prompts = new HashMap<>();
prompts.put(Constants.PromptKey.DAY_ANALYSIS, "Day Trading Analysis");
```

### 11. Alert Type (`Constants.AlertType`)
```java
Constants.AlertType.CALL_SMS_TELEGRAM  // High confidence
Constants.AlertType.SMS_TELEGRAM       // Medium confidence
Constants.AlertType.LOG_ONLY           // Low confidence
```

### 12. API Response Status (`Constants.Api`)
```java
Constants.Api.SUCCESS_STATUS   // "success"
Constants.Api.ERROR_STATUS     // "error"
Constants.Api.WARNING_STATUS   // "warning"
```

**Usage Example:**
```java
return ApiResponse.builder()
    .status(Constants.Api.SUCCESS_STATUS)
    .data(result)
    .build();
```

### 13. Time Zones (`Constants.TimeZones`)
```java
Constants.TimeZones.NEW_YORK  // "America/New_York"
Constants.TimeZones.UTC       // "UTC"
```

### 14. Defaults (`Constants.Defaults`)
```java
Constants.Defaults.DEFAULT_PAGE_SIZE          // 20
Constants.Defaults.MAX_PAGE_SIZE              // 100
Constants.Defaults.DEFAULT_TIMEOUT_SECONDS    // 60
Constants.Defaults.DEFAULT_MAX_RETRIES        // 3
```

## Common Patterns

### Pattern 1: Setting Status
```java
// ❌ BAD
trade.setStatus("IDENTIFIED");

// ✅ GOOD
trade.setStatus(Constants.TradeStatus.IDENTIFIED);
```

### Pattern 2: Checking Status
```java
// ❌ BAD
if ("ACTIVE".equals(conversation.getStatus())) { }

// ✅ GOOD
if (Constants.ConversationStatus.ACTIVE.equals(conversation.getStatus())) { }
```

### Pattern 3: Repository Queries
```java
// ❌ BAD
repository.findByStatus("IDENTIFIED");

// ✅ GOOD
repository.findByStatus(Constants.TradeStatus.IDENTIFIED);
```

### Pattern 4: Building Objects
```java
// ❌ BAD
conversation.builder()
    .status("ACTIVE")
    .type("TRADE_FOLLOWUP")
    .build();

// ✅ GOOD
conversation.builder()
    .status(Constants.ConversationStatus.ACTIVE)
    .type(Constants.ConversationType.TRADE_FOLLOWUP)
    .build();
```

## When NOT to Use Constants

1. **User-facing messages**: Display strings should NOT be constants
   ```java
   // ✅ This is OK
   log.info("Trade identified for symbol: {}", symbol);
   ```

2. **Configuration values**: Values from application.properties
   ```java
   @Value("${ai.trade-finder.enabled:true}")
   private boolean enabled;
   ```

3. **MongoDB Query Strings**: Literal strings in @Query annotations
   ```java
   @Query("{'status': 'IDENTIFIED', 'expiresAt': {$lt: ?0}}")
   ```

## Migration Checklist

When refactoring code to use constants:

- [ ] Add Constants import
- [ ] Replace all status string literals
- [ ] Replace all type string literals
- [ ] Update builder patterns
- [ ] Update repository queries (method-based only)
- [ ] Test compilation
- [ ] Verify functionality

## Benefits of Using Constants

1. **Type Safety**: IDE autocomplete helps prevent typos
2. **Refactoring**: Change value in one place, updates everywhere
3. **Documentation**: Constants serve as documentation
4. **Maintenance**: Easier to understand and maintain code

## Questions?

- Check the full Constants class: `src/main/java/com/trade/app/util/Constants.java`
- See refactoring summary: `docs/STRING_CONSTANTS_REFACTORING_SUMMARY.md`

