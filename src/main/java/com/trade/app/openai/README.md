# OpenAI Integration Layer

## Overview

This package provides a comprehensive integration with OpenAI's API using the official **openai-java** client library. The implementation follows Spring Boot best practices and provides a clean, enterprise-grade abstraction for AI-powered reasoning capabilities.

## Architecture

```
com.trade.app.openai/
├── client/
│   └── AIClientService.java              # Service interface
├── config/
│   └── OpenAIClientConfiguration.java    # Spring configuration
├── dto/
│   ├── AIRequestDTO.java                 # Request data transfer object
│   └── AIResponseDTO.java                # Response data transfer object
├── exception/
│   ├── AIClientException.java            # Base exception
│   ├── AIRequestValidationException.java # Validation errors
│   └── AIResponseParsingException.java   # Parsing errors
├── impl/
│   └── OpenAIClientServiceImpl.java      # Service implementation
├── mapper/
│   └── OpenAIMapper.java                 # DTO <-> SDK mapping
└── example/
    └── OpenAIClientExample.java          # Usage examples
```

## Features

- ✅ Official **openai-java** SDK integration
- ✅ Spring Boot configuration with properties binding
- ✅ Automatic retries with exponential backoff
- ✅ Comprehensive error handling and custom exceptions
- ✅ Request/response logging (configurable)
- ✅ Type-safe DTOs for requests and responses
- ✅ Support for reasoning models (o1, o1-preview, o1-mini)
- ✅ Health check capability
- ✅ Request correlation via IDs
- ✅ Token usage tracking
- ✅ Metadata support for tracking and analytics

## Configuration

### application.properties

Add the following configuration to your `application.properties`:

```properties
# OpenAI API Configuration
openai.api-key=${OPENAI_API_KEY:your-api-key-here}
openai.base-url=https://api.openai.com/v1
openai.org-id=${OPENAI_ORG_ID:}
openai.default-model=o1
openai.timeout-seconds=60
openai.max-retries=3
openai.logging-enabled=false
```

### Environment Variables

For production, use environment variables:

```bash
export OPENAI_API_KEY=sk-...
export OPENAI_ORG_ID=org-...  # Optional
```

### Configuration Properties

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `openai.api-key` | OpenAI API key | - | Yes |
| `openai.base-url` | API base URL | `https://api.openai.com/v1` | No |
| `openai.org-id` | Organization ID | - | No |
| `openai.default-model` | Default model to use | `o1` | No |
| `openai.timeout-seconds` | Request timeout | `60` | No |
| `openai.max-retries` | Max retry attempts | `3` | No |
| `openai.logging-enabled` | Enable request/response logging | `false` | No |

## Usage

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class TradeAnalysisService {
    
    private final AIClientService aiClientService;
    
    public String analyzeStock(String stockSymbol) throws AIClientException {
        // Create request
        AIRequestDTO request = AIRequestDTO.builder()
                .input("Analyze " + stockSymbol + " stock for trading opportunities")
                .maxTokens(500)
                .build();
        
        // Send request
        AIResponseDTO response = aiClientService.sendReasoningRequest(request);
        
        // Return output
        return response.getOutput();
    }
}
```

### With System Instructions

```java
AIRequestDTO request = AIRequestDTO.builder()
        .systemInstructions("You are a financial analyst. Provide concise insights.")
        .input("Should I buy TSLA stock?")
        .maxTokens(400)
        .temperature(0.7)
        .build();

AIResponseDTO response = aiClientService.sendReasoningRequest(request);
```

### With Custom Model

```java
AIRequestDTO request = AIRequestDTO.builder()
        .input("Compare momentum vs value investing")
        .maxTokens(300)
        .build();

// Use a specific model
AIResponseDTO response = aiClientService.sendReasoningRequest(request, "o1-mini");
```

### With Metadata

```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("userId", "user-123");
metadata.put("feature", "trade-analysis");

AIRequestDTO request = AIRequestDTO.builder()
        .input("Analyze AAPL stock")
        .metadata(metadata)
        .build();

AIResponseDTO response = aiClientService.sendReasoningRequest(request);
```

### Error Handling

```java
try {
    AIResponseDTO response = aiClientService.sendReasoningRequest(request);
    // Process response
} catch (AIRequestValidationException e) {
    // Handle validation errors (bad input)
    log.error("Invalid request: {}", e.getMessage());
} catch (AIResponseParsingException e) {
    // Handle parsing errors (unexpected response format)
    log.error("Failed to parse response: {}", e.getMessage());
} catch (AIClientException e) {
    // Handle general API errors
    log.error("API error - Status: {}, Code: {}, Request ID: {}",
            e.getStatusCode(),
            e.getErrorCode(),
            e.getRequestId());
    
    if (e.isRetryable()) {
        // Error is retryable (5xx, 429)
        log.info("Error is retryable");
    }
}
```

### Health Check

```java
@Service
@RequiredArgsConstructor
public class HealthService {
    
    private final AIClientService aiClientService;
    
    public boolean checkAIHealth() {
        return aiClientService.isHealthy();
    }
}
```

## Request DTO Fields

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `input` | String | Main content/question | Yes |
| `systemInstructions` | String | System context | No |
| `maxTokens` | Integer | Max tokens to generate | No |
| `temperature` | Double | Response randomness (0-2) | No |
| `requestId` | String | Unique request identifier | No (auto-generated) |
| `metadata` | Map | Additional tracking data | No |
| `responseFormat` | ResponseFormat | Output format spec | No |

## Response DTO Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Response identifier |
| `model` | String | Model used |
| `output` | String | Generated content |
| `createdAt` | Instant | Creation timestamp |
| `status` | String | Response status |
| `finishReason` | String | Why generation stopped |
| `usage` | Usage | Token usage statistics |
| `metadata` | Map | Response metadata |
| `requestId` | String | Correlating request ID |
| `processingTimeMs` | Long | Processing duration |

## Supported Models

- `o1` - Latest reasoning model
- `o1-preview` - Preview version with extended capabilities
- `o1-mini` - Faster, cost-effective reasoning model

## Retry Strategy

The implementation includes automatic retries with exponential backoff:

- **Initial backoff**: 1 second
- **Max backoff**: 10 seconds
- **Multiplier**: 2.0
- **Max attempts**: Configurable (default: 3)

Retryable errors:
- HTTP 5xx (server errors)
- HTTP 429 (rate limits)
- Network timeouts

## Logging

When `openai.logging-enabled=true`:
- Request inputs are logged at DEBUG level
- Response outputs are logged (truncated to 200 chars)
- All requests include request IDs for correlation

**Security Note**: Disable logging in production to avoid exposing sensitive data.

## Best Practices

1. **Use Request IDs**: Always provide or log request IDs for tracking
2. **Set Appropriate Timeouts**: Reasoning models can take longer
3. **Handle Retryable Errors**: Implement circuit breakers for production
4. **Monitor Token Usage**: Track costs via `response.getUsage()`
5. **Validate Inputs**: Check inputs before sending to avoid wasted requests
6. **Use System Instructions**: Guide model behavior for consistent results
7. **Choose Right Model**: Use `o1-mini` for faster, cheaper responses

## Testing

### Unit Tests

```java
@SpringBootTest
class OpenAIClientServiceTest {
    
    @Autowired
    private AIClientService aiClientService;
    
    @Test
    void testSimpleRequest() throws AIClientException {
        AIRequestDTO request = AIRequestDTO.builder()
                .input("Test input")
                .maxTokens(50)
                .build();
        
        AIResponseDTO response = aiClientService.sendReasoningRequest(request);
        
        assertNotNull(response);
        assertTrue(response.isSuccessful());
    }
}
```

### Integration Tests

```java
@Test
void testWithRealAPI() throws AIClientException {
    // Requires valid API key
    AIRequestDTO request = AIRequestDTO.builder()
            .input("What is 2+2?")
            .maxTokens(50)
            .build();
    
    AIResponseDTO response = aiClientService.sendReasoningRequest(request);
    
    assertNotNull(response.getOutput());
    assertTrue(response.getOutput().contains("4"));
}
```

## Performance Considerations

- **Token Limits**: Monitor `maxTokens` to control costs
- **Temperature**: Lower values (0.1-0.5) for deterministic outputs
- **Model Selection**: `o1-mini` is faster and cheaper than `o1`
- **Caching**: Consider caching responses for repeated queries
- **Batch Processing**: Group similar requests when possible

## Troubleshooting

### Common Issues

1. **"API key not configured"**
   - Set `OPENAI_API_KEY` environment variable
   - Or configure `openai.api-key` in properties

2. **"Authentication error"**
   - Verify API key is valid
   - Check if key has required permissions

3. **"Rate limit exceeded"**
   - Automatic retries are enabled
   - Consider implementing rate limiting on your side

4. **"Timeout"**
   - Increase `openai.timeout-seconds`
   - Reasoning models can take 30-60+ seconds

5. **"Failed to parse response"**
   - Check OpenAI API status
   - Enable logging to see raw responses

## References

- [OpenAI Java SDK](https://github.com/openai/openai-java)
- [OpenAI API Documentation](https://platform.openai.com/docs)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## License

Part of AI Trade Finder project.

## Support

For issues or questions, contact the AI Trade Finder development team.
