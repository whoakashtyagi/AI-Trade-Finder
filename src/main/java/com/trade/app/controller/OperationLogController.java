package com.trade.app.controller;

import com.trade.app.persistence.mongo.document.OperationLog;
import com.trade.app.persistence.mongo.repository.OperationLogRepository;
import com.trade.app.service.OperationLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/operations")
@RequiredArgsConstructor
@Validated
public class OperationLogController {

    private final OperationLogRepository repository;
    private final OperationLogService operationLogService;

    /**
     * Query operation logs for UI.
     *
     * GET /api/v2/operations/logs?operationId=...&operationType=...&status=...&source=...&from=...&to=...&limit=200
     */
    @GetMapping("/logs")
    public ResponseEntity<List<OperationLog>> queryLogs(
        @RequestParam(required = false) String operationId,
        @RequestParam(required = false) String operationType,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String source,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "200") @Min(1) @Max(1000) int limit
    ) {
        if (operationId != null && !operationId.isBlank()) {
            return ResponseEntity.ok(operationLogService.getLogsForOperation(operationId));
        }

        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt"));

        if (from != null && to != null) {
            return ResponseEntity.ok(repository.findByStartedAtBetweenOrderByStartedAtDesc(from, to, page).getContent());
        }
        if (operationType != null && !operationType.isBlank()) {
            return ResponseEntity.ok(repository.findByOperationTypeOrderByStartedAtDesc(operationType, page).getContent());
        }
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(repository.findByStatusOrderByStartedAtDesc(status, page).getContent());
        }
        if (source != null && !source.isBlank()) {
            return ResponseEntity.ok(repository.findBySourceOrderByStartedAtDesc(source, page).getContent());
        }

        return ResponseEntity.ok(repository.findAll(page).getContent());
    }

    /**
     * Get the latest log record for a correlation operationId.
     */
    @GetMapping("/{operationId}")
    public ResponseEntity<OperationLog> getLatestByOperationId(@PathVariable String operationId) {
        Optional<OperationLog> latest = repository.findFirstByOperationIdOrderByStartedAtDesc(operationId);
        return latest.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get a specific log document by Mongo _id.
     */
    @GetMapping("/logs/{id}")
    public ResponseEntity<OperationLog> getById(@PathVariable String id) {
        return repository.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
