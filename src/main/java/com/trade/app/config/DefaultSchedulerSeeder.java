package com.trade.app.config;

import com.trade.app.persistence.mongo.document.SchedulerConfig;
import com.trade.app.persistence.mongo.repository.SchedulerConfigRepository;
import com.trade.app.scheduler.DynamicSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Seeds default scheduler configurations on application startup.
 * Creates standard trade finders and maintenance tasks if they don't exist.
 * 
 * @author AI Trade Finder Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultSchedulerSeeder {

    private final SchedulerConfigRepository schedulerConfigRepository;
    private final DynamicSchedulerService dynamicSchedulerService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedDefaultSchedulers() {
        log.info("Starting default scheduler seeding...");
        
        try {
            createOrUpdateScheduler(createMainTradeFinderScheduler());
            createOrUpdateScheduler(createTradeExpirationScheduler());
            createOrUpdateScheduler(createStatisticsLoggerScheduler());
            createOrUpdateScheduler(createNightlyCleanupScheduler());
            
            log.info("Default scheduler seeding completed successfully");
        } catch (Exception e) {
            log.error("Error seeding default schedulers", e);
        }
    }

    private void createOrUpdateScheduler(SchedulerConfig config) {
        schedulerConfigRepository.findByName(config.getName())
            .ifPresentOrElse(
                existing -> {
                    log.info("Scheduler '{}' already exists, skipping", config.getName());
                },
                () -> {
                    SchedulerConfig saved = schedulerConfigRepository.save(config);
                    log.info("Created scheduler: {} (ID: {})", saved.getName(), saved.getId());
                    
                    // Schedule the task immediately
                    if (saved.isEnabled()) {
                        try {
                            dynamicSchedulerService.scheduleTask(saved);
                            log.info("Successfully scheduled: {}", saved.getName());
                        } catch (Exception e) {
                            log.error("Failed to schedule: {}", saved.getName(), e);
                        }
                    }
                }
            );
    }

    /**
     * Main trade finder - runs every 5 minutes for all major symbols
     */
    private SchedulerConfig createMainTradeFinderScheduler() {
        return SchedulerConfig.builder()
            .name("trade-finder-main")
            .description("Primary trade finder for NQ, ES, YM, RTY - every 5 minutes")
            .type("TRADE_FINDER")
            .enabled(true)
            .scheduleExpression("0 */5 * * * *") // Every 5 minutes
            .scheduleType("CRON")
            .parameters(Map.of(
                "symbols", List.of("NQ", "ES", "YM", "RTY"),
                "timeframes", List.of("5m", "15m"),
                "minConfidence", 70,
                "description", "Main trade finder with standard confidence threshold"
            ))
            .handlerBean("aiTradeFinderService")
            .handlerMethod("findTrades")
            .priority(10)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    /**
     * Trade expiration checker - runs every 15 minutes
     */
    private SchedulerConfig createTradeExpirationScheduler() {
        return SchedulerConfig.builder()
            .name("trade-expiration-checker")
            .description("Marks expired trades as inactive - every 15 minutes")
            .type("MAINTENANCE")
            .enabled(true)
            .scheduleExpression("0 */15 * * * *") // Every 15 minutes
            .scheduleType("CRON")
            .parameters(Map.of(
                "expirationHours", 8,
                "batchSize", 100,
                "description", "Cleans up expired trades to maintain data freshness"
            ))
            .handlerBean("tradeLifecycleScheduler")
            .handlerMethod("checkExpiredTrades")
            .priority(5)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    /**
     * Statistics logger - runs every hour
     */
    private SchedulerConfig createStatisticsLoggerScheduler() {
        return SchedulerConfig.builder()
            .name("statistics-logger")
            .description("Logs system statistics - every hour")
            .type("MONITORING")
            .enabled(true)
            .scheduleExpression("0 0 * * * *") // Every hour at :00
            .scheduleType("CRON")
            .parameters(Map.of(
                "logLevel", "INFO",
                "includeDetails", true,
                "description", "Periodic statistics logging for monitoring"
            ))
            .handlerBean("statisticsService")
            .handlerMethod("logStatistics")
            .priority(3)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    /**
     * Nightly cleanup - runs at 2 AM
     */
    private SchedulerConfig createNightlyCleanupScheduler() {
        return SchedulerConfig.builder()
            .name("nightly-cleanup")
            .description("Daily cleanup of old data - runs at 2 AM")
            .type("MAINTENANCE")
            .enabled(true)
            .scheduleExpression("0 0 2 * * *") // 2:00 AM daily
            .scheduleType("CRON")
            .parameters(Map.of(
                "retentionDays", 30,
                "cleanupTypes", List.of("trades", "chat_sessions", "events"),
                "dryRun", false,
                "description", "Removes data older than 30 days"
            ))
            .handlerBean("cleanupService")
            .handlerMethod("performNightlyCleanup")
            .priority(2)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
