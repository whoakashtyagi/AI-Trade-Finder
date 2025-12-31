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
import java.util.Map;

/**
 * MongoDB document for dynamic scheduler configuration.
 * 
 * Enables runtime configuration of schedulers without code deployment.
 * Schedulers can be created, updated, enabled/disabled via API.
 * 
 * @author AI Trade Finder Team
 */
@Document("scheduler_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerConfig {

    @Id
    private String id;

    /**
     * Unique name for the scheduler (e.g., "trade-finder-nq", "expiration-checker").
     */
    @Indexed(unique = true)
    @Field("name")
    private String name;

    /**
     * Human-readable description.
     */
    @Field("description")
    private String description;

    /**
     * Scheduler type (e.g., "TRADE_FINDER", "EXPIRATION", "STATISTICS", "CUSTOM").
     */
    @Indexed
    @Field("type")
    private String type;

    /**
     * Enable/disable the scheduler.
     */
    @Field("enabled")
    private boolean enabled;

    /**
     * Cron expression or fixed rate in milliseconds.
     * Examples: 0 *&#47;5 * * * * (cron every 5 minutes) or 300000 (fixed rate 5 minutes)
     */
    @Field("schedule_expression")
    private String scheduleExpression;

    /**
     * Schedule type: "CRON" or "FIXED_RATE" or "FIXED_DELAY".
     */
    @Field("schedule_type")
    private String scheduleType;

    /**
     * Configuration parameters for the scheduler (JSON-like structure).
     */
    @Field("parameters")
    private Map<String, Object> parameters;

    /**
     * Target handler bean name (Spring bean that executes the task).
     */
    @Field("handler_bean")
    private String handlerBean;

    /**
     * Target handler method name.
     */
    @Field("handler_method")
    private String handlerMethod;

    /**
     * Priority (higher number = higher priority).
     */
    @Field("priority")
    private Integer priority;

    /**
     * Last execution timestamp.
     */
    @Field("last_execution")
    private Instant lastExecution;

    /**
     * Next scheduled execution.
     */
    @Field("next_execution")
    private Instant nextExecution;

    /**
     * Execution count.
     */
    @Field("execution_count")
    private Long executionCount;

    /**
     * Failure count.
     */
    @Field("failure_count")
    private Long failureCount;

    /**
     * Last execution status.
     */
    @Field("last_status")
    private String lastStatus;

    /**
     * Last error message (if any).
     */
    @Field("last_error")
    private String lastError;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt;

    @Field("created_by")
    private String createdBy;
}
