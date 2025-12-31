# OpenAI Responses API - Complete Implementation Guide

## Overview

This project now fully implements OpenAI's **Responses API** (the modern, recommended approach) with complete support for:

- ✅ Response ID storage and retrieval
- ✅ Conversation state management with `previous_response_id`
- ✅ Multi-turn conversations with context preservation
- ✅ Response storage (`store` parameter)
- ✅ Metadata tracking for all requests
- ✅ Background processing support
- ✅ Safety identifiers and prompt caching
- ✅ MongoDB-backed conversation persistence

## Architecture

### Services Overview

#### 1. **AIClientService** (Interface)
- Generic interface for AI operations
- Implementation: `OpenAIClientServiceImpl`
- Uses official OpenAI Java SDK (v4.13.0)

#### 2. **AITradeFinderService** (Automated Trade Finding)
- **Purpose**: Scheduled, automated trade signal generation
- **Runs**: Every 5 minutes (configurable)
- **Use Case**: Autonomous trade discovery based on events and OHLC data
- **NOT REDUNDANT** - Completely different purpose from AIWorkflowService

#### 3. **AIWorkflowService** (Manual Analysis)
- **Purpose**: On-demand, user-initiated market analysis
- **Runs**: Via REST API when user requests
- **Use Case**: Custom prompts, flexible data sources, multi-turn conversations
- **NOT REDUNDANT** - User-facing analysis tool

#### 4. **AIConversationService** (NEW)
- **Purpose**: Manages conversation state for multi-turn interactions
- **Features**:
  - Creates conversation sessions
  - Tracks response IDs from each turn
  - Provides latest response ID for conversation continuity
  - Cleans up expired conversations
  - Tracks usage statistics

### Data Flow

```
User Request → AIWorkflowService
                    ↓
              Build Prompt + Market Data
                    ↓
              Check for Previous Conversation
                    ↓ (if continuing)
          AIConversationService.getLatestResponseId()
                    ↓
         AIRequestDTO (with previousResponseId)
                    ↓
         OpenAIClientServiceImpl → OpenAI API
                    ↓
              AIResponseDTO (with response.id)
                    ↓
         AIConversationService.addTurn()
                    ↓
              MongoDB: ai_conversations
```

## Key Features

### 1. Response ID Storage

Every OpenAI response includes a unique ID that can be used for:
- **Conversation continuity**: Pass as `previousResponseId` to maintain context
- **Response retrieval**: Get specific responses later via API
- **Audit trails**: Track conversation history
- **Usage analytics**: Measure token consumption per conversation

### 2. Conversation State

**MongoDB Document: `AIConversation`**
```java
{
  "_id": ObjectId,
  "conversationId": "uuid",
  "symbol": "NQ",
  "conversationType": "TRADE_ANALYSIS",
  "status": "ACTIVE",
  "turns": [
    {
      "responseId": "resp_abc123",
      "requestId": "req_xyz789",
      "userInputSummary": "Analyze NQ for day trading",
      "aiOutputSummary": "Based on the analysis...",
      "model": "gpt-4o",
      "timestamp": "2025-12-31T10:00:00Z",
      "tokensUsed": 1234
    }
  ],
  "createdAt": "2025-12-31T09:00:00Z",
  "lastActivityAt": "2025-12-31T10:00:00Z",
  "expiresAt": "2026-01-01T09:00:00Z"
}
```

### 3. Multi-Turn Conversations

**Example Flow:**

**Turn 1:**
```json
POST /api/v1/ai/analyze
{
  "input": "What's the current trend for NQ?",
  "conversationId": "conv_123"
}
Response: { "id": "resp_abc", "output": "NQ is in an uptrend..." }
```

**Turn 2 (with context):**
```json
POST /api/v1/ai/analyze
{
  "input": "What entry price would you recommend?",
  "conversationId": "conv_123"
}
// Behind the scenes:
// - Service retrieves previous response ID: "resp_abc"
// - Sends request with previousResponseId: "resp_abc"
// - OpenAI maintains full context from Turn 1
```

## API Endpoints

### Conversation Management

#### Create Conversation
```bash
POST /api/v1/conversations
Content-Type: application/json

{
  "conversationType": "TRADE_ANALYSIS",
  "symbol": "NQ",
  "userId": "user123",
  "expiryHours": 24
}
```

#### Get Conversation
```bash
GET /api/v1/conversations/{conversationId}
```

#### Get Conversation Stats
```bash
GET /api/v1/conversations/{conversationId}/stats

Response:
{
  "conversationId": "conv_123",
  "turnCount": 5,
  "totalTokens": 12450,
  "createdAt": "2025-12-31T09:00:00Z",
  "status": "ACTIVE"
}
```

#### Get Conversations by Symbol
```bash
GET /api/v1/conversations/symbol/NQ
```

#### Complete Conversation
```bash
POST /api/v1/conversations/{conversationId}/complete
```

#### Cleanup Expired
```bash
POST /api/v1/conversations/cleanup
```

### AI Analysis Endpoints

#### Standard Analysis
```bash
POST /api/v1/ai/analyze
Content-Type: application/json

{
  "input": "Analyze NQ for day trading opportunities",
  "systemInstructions": "You are an expert day trader",
  "maxTokens": 2000,
  "temperature": 0.7,
  "metadata": {
    "symbol": "NQ",
    "userId": "user123"
  }
}
```

#### Multi-Turn Analysis (with conversation)
```bash
POST /api/v1/ai/analyze
{
  "input": "What are the entry levels?",
  "previousResponseId": "resp_abc123",
  "store": true
}
```

### AI Workflow Endpoints

#### Execute Workflow with Conversation
```bash
POST /api/v1/ai/workflow/execute
{
  "symbol": "NQ",
  "promptType": "PREDEFINED",
  "selectedPredefinedPrompt": "day_analysis",
  "conversationId": "conv_123",
  "timeframeSettings": {
    "5m": { "enabled": true, ... },
    "1h": { "enabled": true, ... }
  }
}
```

## Configuration

### Application Properties

```properties
# OpenAI API Configuration
openai.api-key=${OPENAI_API_KEY}
openai.base-url=https://api.openai.com/v1
openai.default-model=gpt-4o
openai.timeout-seconds=60
openai.max-retries=3
openai.logging-enabled=false

# MongoDB
spring.mongodb.uri=mongodb://...

# Conversation Settings
ai.conversation.default-expiry-hours=24
ai.conversation.cleanup-interval-hours=6
```

## Best Practices

### 1. Always Enable Response Storage
```java
AIRequestDTO.builder()
    .input("Your prompt")
    .store(true)  // ← Always enable for conversation tracking
    .build();
```

### 2. Use Metadata for Tracking
```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("symbol", "NQ");
metadata.put("userId", userId);
metadata.put("timestamp", Instant.now().toString());

AIRequestDTO.builder()
    .input("Your prompt")
    .metadata(metadata)
    .build();
```

### 3. Manage Conversation Lifecycle
```java
// Create conversation at start of session
AIConversation conv = conversationService.createConversation(
    "TRADE_ANALYSIS", "NQ", userId, 24
);

// Use conversation ID in subsequent requests
AIWorkflowRequest request = AIWorkflowRequest.builder()
    .conversationId(conv.getConversationId())
    // ... other fields
    .build();

// Complete when done
conversationService.completeConversation(conv.getConversationId());
```

### 4. Regular Cleanup
Schedule a job to clean up expired conversations:
```java
@Scheduled(fixedRate = 21600000) // Every 6 hours
public void cleanupConversations() {
    conversationService.cleanupExpiredConversations();
}
```

## Response API vs Chat Completions

### Why Responses API?

| Feature | Responses API | Chat Completions (Legacy) |
|---------|---------------|---------------------------|
| Conversation State | ✅ Built-in with `previous_response_id` | ❌ Manual history management |
| Response Storage | ✅ `store` parameter | ❌ Not available |
| Response IDs | ✅ Unique IDs for retrieval | ⚠️ Limited |
| Metadata | ✅ First-class support | ⚠️ Limited |
| Multi-turn | ✅ Optimized | ⚠️ Manual |
| Future-proof | ✅ Latest API | ⚠️ Legacy |

## Code Examples

### Example 1: Simple One-Off Analysis
```java
AIRequestDTO request = AIRequestDTO.builder()
    .input("Analyze NQ market structure")
    .systemInstructions("You are an expert trader")
    .temperature(0.7)
    .store(false) // One-off, no need to store
    .build();

AIResponseDTO response = aiClientService.sendReasoningRequest(request);
```

### Example 2: Multi-Turn Conversation
```java
// Turn 1
AIConversation conv = conversationService.createConversation(
    "TRADE_ANALYSIS", "NQ", "user123", 24
);

AIRequestDTO request1 = AIRequestDTO.builder()
    .input("What's the current market structure for NQ?")
    .store(true)
    .build();

AIResponseDTO response1 = aiClientService.sendReasoningRequest(request1);
conversationService.addTurn(conv.getConversationId(), response1, 
    request1.getRequestId(), "Market structure query");

// Turn 2 (with context from Turn 1)
String previousId = conversationService.getLatestResponseId(conv.getConversationId());

AIRequestDTO request2 = AIRequestDTO.builder()
    .input("Based on that structure, what entry levels would you recommend?")
    .previousResponseId(previousId)
    .store(true)
    .build();

AIResponseDTO response2 = aiClientService.sendReasoningRequest(request2);
conversationService.addTurn(conv.getConversationId(), response2, 
    request2.getRequestId(), "Entry level query");
```

### Example 3: Background Processing
```java
AIRequestDTO request = AIRequestDTO.builder()
    .input("Perform deep analysis on 1000 candles")
    .background(true) // Run in background
    .store(true)
    .build();

AIResponseDTO response = aiClientService.sendReasoningRequest(request);
// Response status will be "in_progress" or "queued"
// Poll for completion using response.getId()
```

## Monitoring & Analytics

### Track Conversation Usage
```java
Map<String, Object> stats = conversationService.getConversationStats(conversationId);
System.out.println("Turns: " + stats.get("turnCount"));
System.out.println("Total Tokens: " + stats.get("totalTokens"));
```

### Track Response Details
```java
AIResponseDTO response = // ... from API call
System.out.println("Response ID: " + response.getId());
System.out.println("Model: " + response.getModel());
System.out.println("Status: " + response.getStatus());
System.out.println("Tokens: " + response.getUsage().getTotalTokens());
System.out.println("Processing Time: " + response.getProcessingTimeMs() + "ms");
```

## Migration Guide

If you have existing code using Chat Completions:

### Before (Chat Completions):
```java
ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
    .addUserMessage("Hello")
    .model(ChatModel.GPT_4_O)
    .build();
ChatCompletion completion = client.chat().completions().create(params);
```

### After (Responses API):
```java
ResponseCreateParams params = ResponseCreateParams.builder()
    .input("Hello")
    .model("gpt-4o")
    .store(true)
    .build();
Response response = client.responses().create(params);
```

## Troubleshooting

### Issue: Response ID is null
- Ensure `store=true` in request
- Check OpenAI SDK version (must be ≥4.13.0)

### Issue: Conversation context not maintained
- Verify `previousResponseId` is being set
- Check conversation exists and has turns
- Ensure response was stored (`store=true`)

### Issue: Tokens growing rapidly
- Implement conversation compaction (see OpenAI docs)
- Set reasonable expiry times
- Complete conversations when done

## References

- [OpenAI Responses API Documentation](https://platform.openai.com/docs/api-reference/responses)
- [OpenAI Java SDK](https://github.com/openai/openai-java)
- [Conversation State Guide](https://platform.openai.com/docs/guides/conversation-state)

## Summary

✅ **Not Redundant**: AITradeFinderService and AIWorkflowService serve different purposes  
✅ **Response API**: Fully implemented with all modern features  
✅ **Response IDs**: Stored in MongoDB via AIConversation documents  
✅ **Multi-Turn**: Full support for conversation continuity  
✅ **Clean Code**: Proper separation of concerns, DI, and best practices  
✅ **Production Ready**: Includes monitoring, cleanup, and error handling
