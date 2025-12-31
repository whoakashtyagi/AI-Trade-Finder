package com.trade.app.controller;

import com.trade.app.persistence.mongo.document.SchedulerConfig;
import com.trade.app.persistence.mongo.repository.SchedulerConfigRepository;
import com.trade.app.scheduler.DynamicSchedulerService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing dynamic scheduler configurations.
 * 
 * Features:
 * - Create/update/delete schedulers dynamically
 * - Enable/disable schedulers at runtime
 * - View scheduler status and statistics
 * - Test scheduler execution
 * 
 * @author AI Trade Finder Team
 */
@RestController
@RequestMapping("/api/v2/schedulers")
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfigController {

    private final SchedulerConfigRepository schedulerConfigRepository;
    private final DynamicSchedulerService dynamicSchedulerService;

    /**
     * Create a new scheduler configuration.
     * 
     * POST /api/v2/schedulers
     */
    @PostMapping
    public ResponseEntity<SchedulerConfig> createScheduler(@RequestBody SchedulerConfig config) {
        try {
            log.info("Creating new scheduler: {}", config.getName());

            if (schedulerConfigRepository.existsByName(config.getName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            config.setCreatedAt(Instant.now());
            config.setExecutionCount(0L);
            config.setFailureCount(0L);

            SchedulerConfig saved = schedulerConfigRepository.save(config);

            // Start scheduler if enabled
            if (saved.isEnabled()) {
                dynamicSchedulerService.scheduleTask(saved);
            }

            log.info("Created scheduler: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {
            log.error("Error creating scheduler", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all scheduler configurations.
     * 
     * GET /api/v2/schedulers
     */
    @GetMapping
    public ResponseEntity<List<SchedulerConfig>> getAllSchedulers() {
        List<SchedulerConfig> schedulers = schedulerConfigRepository.findAll();
        return ResponseEntity.ok(schedulers);
    }

    /**
     * Get scheduler by name.
     * 
     * GET /api/v2/schedulers/{name}
     */
    @GetMapping("/{name}")
    public ResponseEntity<SchedulerConfig> getScheduler(@PathVariable String name) {
        return schedulerConfigRepository.findByName(name)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update scheduler configuration.
     * 
     * PUT /api/v2/schedulers/{name}
     */
    @PutMapping("/{name}")
    public ResponseEntity<SchedulerConfig> updateScheduler(
        @PathVariable String name,
        @RequestBody SchedulerConfig config
    ) {
        try {
            log.info("Updating scheduler: {}", name);

            SchedulerConfig existing = schedulerConfigRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Scheduler not found"));

            // Update fields
            existing.setDescription(config.getDescription());
            existing.setEnabled(config.isEnabled());
            existing.setScheduleExpression(config.getScheduleExpression());
            existing.setScheduleType(config.getScheduleType());
            existing.setParameters(config.getParameters());
            existing.setHandlerBean(config.getHandlerBean());
            existing.setHandlerMethod(config.getHandlerMethod());
            existing.setPriority(config.getPriority());
            existing.setUpdatedAt(Instant.now());

            SchedulerConfig updated = schedulerConfigRepository.save(existing);

            // Reschedule
            dynamicSchedulerService.rescheduleTask(updated);

            log.info("Updated scheduler: {}", name);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            log.error("Error updating scheduler", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete scheduler configuration.
     * 
     * DELETE /api/v2/schedulers/{name}
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteScheduler(@PathVariable String name) {
        try {
            log.info("Deleting scheduler: {}", name);

            SchedulerConfig config = schedulerConfigRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Scheduler not found"));

            // Cancel scheduled task
            dynamicSchedulerService.cancelTask(name);

            // Delete from database
            schedulerConfigRepository.delete(config);

            log.info("Deleted scheduler: {}", name);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Error deleting scheduler", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Enable scheduler.
     * 
     * POST /api/v2/schedulers/{name}/enable
     */
    @PostMapping("/{name}/enable")
    public ResponseEntity<SchedulerConfig> enableScheduler(@PathVariable String name) {
        try {
            log.info("Enabling scheduler: {}", name);

            SchedulerConfig config = schedulerConfigRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Scheduler not found"));

            config.setEnabled(true);
            config.setUpdatedAt(Instant.now());
            SchedulerConfig updated = schedulerConfigRepository.save(config);

            dynamicSchedulerService.scheduleTask(updated);

            log.info("Enabled scheduler: {}", name);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            log.error("Error enabling scheduler", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Disable scheduler.
     * 
     * POST /api/v2/schedulers/{name}/disable
     */
    @PostMapping("/{name}/disable")
    public ResponseEntity<SchedulerConfig> disableScheduler(@PathVariable String name) {
        try {
            log.info("Disabling scheduler: {}", name);

            SchedulerConfig config = schedulerConfigRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Scheduler not found"));

            config.setEnabled(false);
            config.setUpdatedAt(Instant.now());
            SchedulerConfig updated = schedulerConfigRepository.save(config);

            dynamicSchedulerService.cancelTask(name);

            log.info("Disabled scheduler: {}", name);
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            log.error("Error disabling scheduler", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get scheduler statistics.
     * 
     * GET /api/v2/schedulers/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<SchedulerStatistics> getStatistics() {
        try {
            List<SchedulerConfig> all = schedulerConfigRepository.findAll();
            List<SchedulerConfig> enabled = schedulerConfigRepository.findByEnabled(true);

            long totalExecutions = all.stream()
                .mapToLong(s -> s.getExecutionCount() != null ? s.getExecutionCount() : 0L)
                .sum();

            long totalFailures = all.stream()
                .mapToLong(s -> s.getFailureCount() != null ? s.getFailureCount() : 0L)
                .sum();

            Map<String, Boolean> activeSchedulers = dynamicSchedulerService.getActiveSchedulers();

            SchedulerStatistics stats = SchedulerStatistics.builder()
                .totalSchedulers(all.size())
                .enabledSchedulers(enabled.size())
                .activeSchedulers(activeSchedulers.size())
                .totalExecutions(totalExecutions)
                .totalFailures(totalFailures)
                .schedulerNames(activeSchedulers)
                .build();

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active schedulers.
     * 
     * GET /api/v2/schedulers/active
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Boolean>> getActiveSchedulers() {
        Map<String, Boolean> active = dynamicSchedulerService.getActiveSchedulers();
        return ResponseEntity.ok(active);
    }

    // ========== DTOs ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulerStatistics {
        private Integer totalSchedulers;
        private Integer enabledSchedulers;
        private Integer activeSchedulers;
        private Long totalExecutions;
        private Long totalFailures;
        private Map<String, Boolean> schedulerNames;
    }
}
