package com.trade.app.scheduler;

import com.trade.app.persistence.mongo.document.SchedulerConfig;
import com.trade.app.persistence.mongo.repository.SchedulerConfigRepository;
import com.trade.app.service.OperationLogService;
import com.trade.app.util.OperationLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Dynamic scheduler service that manages runtime-configurable schedulers.
 * 
 * Features:
 * - Create/update/delete schedulers via API
 * - Enable/disable schedulers dynamically
 * - Support for cron expressions and fixed rates
 * - Execution tracking and statistics
 * - Error handling and retry logic
 * 
 * @author AI Trade Finder Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicSchedulerService {

    private final SchedulerConfigRepository schedulerConfigRepository;
    private final TaskScheduler taskScheduler;
    private final ApplicationContext applicationContext;
    private final OperationLogService operationLogService;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Initializes and starts all enabled schedulers on application startup.
     */
    @PostConstruct
    public void initializeSchedulers() {
        log.info("Initializing dynamic schedulers...");
        
        List<SchedulerConfig> configs = schedulerConfigRepository.findByEnabled(true);
        log.info("Found {} enabled scheduler configurations", configs.size());

        for (SchedulerConfig config : configs) {
            try {
                scheduleTask(config);
            } catch (Exception e) {
                log.error("Failed to initialize scheduler: {}", config.getName(), e);
            }
        }

        log.info("Dynamic schedulers initialized successfully");
    }

    /**
     * Cancels all scheduled tasks on application shutdown.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down dynamic schedulers...");
        scheduledTasks.forEach((name, future) -> {
            future.cancel(false);
            log.debug("Cancelled scheduler: {}", name);
        });
        scheduledTasks.clear();
        log.info("Dynamic schedulers shut down complete");
    }

    /**
     * Schedules a task based on configuration.
     */
    public void scheduleTask(SchedulerConfig config) {
        if (!config.isEnabled()) {
            log.info("Scheduler {} is disabled, skipping", config.getName());
            return;
        }

        // Cancel existing task if any
        cancelTask(config.getName());

        log.info("Scheduling task: {} with type: {}", config.getName(), config.getScheduleType());

        Runnable task = createTaskRunnable(config);

        ScheduledFuture<?> scheduledFuture;

        switch (config.getScheduleType().toUpperCase()) {
            case "CRON":
                scheduledFuture = taskScheduler.schedule(task, new CronTrigger(config.getScheduleExpression()));
                break;

            case "FIXED_RATE":
                long rate = Long.parseLong(config.getScheduleExpression());
                scheduledFuture = taskScheduler.scheduleAtFixedRate(task, Duration.ofMillis(rate));
                break;

            case "FIXED_DELAY":
                long delay = Long.parseLong(config.getScheduleExpression());
                scheduledFuture = taskScheduler.scheduleWithFixedDelay(task, Duration.ofMillis(delay));
                break;

            default:
                log.error("Unknown schedule type: {}", config.getScheduleType());
                return;
        }

        scheduledTasks.put(config.getName(), scheduledFuture);
        log.info("Successfully scheduled task: {}", config.getName());
    }

    /**
     * Cancels a scheduled task.
     */
    public void cancelTask(String name) {
        ScheduledFuture<?> future = scheduledTasks.remove(name);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled scheduled task: {}", name);
        }
    }

    /**
     * Reschedules a task (cancel + schedule).
     */
    public void rescheduleTask(SchedulerConfig config) {
        cancelTask(config.getName());
        scheduleTask(config);
    }

    /**
     * Creates a runnable task from configuration.
     */
    private Runnable createTaskRunnable(SchedulerConfig config) {
        return () -> {
            long startTime = System.currentTimeMillis();
            log.info("Executing scheduler: {}", config.getName());

            String operationId = operationLogService.startOrReuse(
                "SCHEDULER_EXECUTION",
                "Scheduler execution: " + config.getName(),
                "SCHEDULER",
                Map.of(
                    "schedulerName", config.getName(),
                    "scheduleType", config.getScheduleType(),
                    "handlerBean", config.getHandlerBean(),
                    "handlerMethod", config.getHandlerMethod()
                )
            );

            operationLogService.addEvent(operationId, "INFO", "Scheduler execution started", Map.of(
                "schedulerName", config.getName()
            ));

            try {
                // Get bean and invoke method
                Object bean = applicationContext.getBean(config.getHandlerBean());
                Method method = bean.getClass().getMethod(config.getHandlerMethod());
                method.invoke(bean);

                // Update execution stats
                updateExecutionStats(config, "SUCCESS", null, System.currentTimeMillis() - startTime);

                log.info("Successfully executed scheduler: {} in {}ms", 
                    config.getName(), System.currentTimeMillis() - startTime);

                operationLogService.completeSuccess(operationId, "Scheduler execution completed", Map.of(
                    "durationMs", System.currentTimeMillis() - startTime,
                    "status", "SUCCESS"
                ));

            } catch (Exception e) {
                log.error("Error executing scheduler: {}", config.getName(), e);
                updateExecutionStats(config, "FAILED", e.getMessage(), System.currentTimeMillis() - startTime);

                operationLogService.completeFailure(operationId, "Scheduler execution failed", e, Map.of(
                    "durationMs", System.currentTimeMillis() - startTime,
                    "status", "FAILED"
                ));
            } finally {
                OperationLogContext.clear();
            }
        };
    }

    /**
     * Updates execution statistics in the database.
     */
    private void updateExecutionStats(SchedulerConfig config, String status, String error, long duration) {
        try {
            SchedulerConfig updated = schedulerConfigRepository.findByName(config.getName())
                .orElse(config);

            updated.setLastExecution(Instant.now());
            updated.setLastStatus(status);
            updated.setLastError(error);
            updated.setExecutionCount(
                (updated.getExecutionCount() != null ? updated.getExecutionCount() : 0L) + 1
            );

            if ("FAILED".equals(status)) {
                updated.setFailureCount(
                    (updated.getFailureCount() != null ? updated.getFailureCount() : 0L) + 1
                );
            }

            schedulerConfigRepository.save(updated);
        } catch (Exception e) {
            log.error("Failed to update scheduler stats for: {}", config.getName(), e);
        }
    }

    /**
     * Gets all active scheduler names.
     */
    public Map<String, Boolean> getActiveSchedulers() {
        Map<String, Boolean> result = new ConcurrentHashMap<>();
        scheduledTasks.forEach((name, future) -> result.put(name, !future.isCancelled()));
        return result;
    }
}
