package com.trade.app.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("operation_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    @Id
    private String id;

    /**
     * Correlation ID for a single end-to-end operation (UUID string).
     */
    @Indexed
    @Field("operation_id")
    private String operationId;

    /**
     * High-level operation type (e.g., SCHEDULER_EXECUTION, TRADE_FINDER_RUN, AI_ANALYZE).
     */
    @Indexed
    @Field("operation_type")
    private String operationType;

    /**
     * Source of operation (e.g., SCHEDULER, MANUAL, API).
     */
    @Indexed
    @Field("source")
    private String source;

    /**
     * Status: IN_PROGRESS, SUCCESS, FAILED.
     */
    @Indexed
    @Field("status")
    private String status;

    @Field("title")
    private String title;

    @Field("message")
    private String message;

    @Field("error")
    private String error;

    @Field("stack_trace")
    private String stackTrace;

    @Indexed
    @Field("started_at")
    private Instant startedAt;

    @Field("ended_at")
    private Instant endedAt;

    @Field("duration_ms")
    private Long durationMs;

    @Field("metadata")
    private Map<String, Object> metadata;

    @Field("events")
    private List<OperationEvent> events;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationEvent {
        @Field("ts")
        private Instant timestamp;

        @Field("level")
        private String level;

        @Field("message")
        private String message;

        @Field("data")
        private Map<String, Object> data;
    }
}
