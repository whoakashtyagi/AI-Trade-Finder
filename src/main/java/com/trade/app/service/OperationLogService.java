package com.trade.app.service;

import com.trade.app.persistence.mongo.document.OperationLog;
import com.trade.app.persistence.mongo.repository.OperationLogRepository;
import com.trade.app.util.OperationLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationLogService {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final OperationLogRepository repository;

    public String startOrReuse(String operationType, String title, String source, Map<String, Object> metadata) {
        String existing = OperationLogContext.getOperationId();
        if (existing != null && !existing.isBlank()) {
            addEvent(existing, "INFO", "Reusing existing operation context", Map.of(
                "operationType", operationType,
                "title", title,
                "source", source
            ));
            return existing;
        }

        String operationId = UUID.randomUUID().toString();
        OperationLogContext.setOperationId(operationId);

        OperationLog logEntry = OperationLog.builder()
            .operationId(operationId)
            .operationType(operationType)
            .source(source)
            .status(STATUS_IN_PROGRESS)
            .title(title)
            .startedAt(Instant.now())
            .metadata(metadata)
            .events(new ArrayList<>())
            .updatedAt(Instant.now())
            .build();

        repository.save(logEntry);
        return operationId;
    }

    public void addEvent(String operationId, String level, String message, Map<String, Object> data) {
        try {
            OperationLog latest = repository.findFirstByOperationIdOrderByStartedAtDesc(operationId)
                .orElse(null);
            if (latest == null) {
                return;
            }

            List<OperationLog.OperationEvent> events = latest.getEvents();
            if (events == null) {
                events = new ArrayList<>();
            }

            events.add(OperationLog.OperationEvent.builder()
                .timestamp(Instant.now())
                .level(level)
                .message(message)
                .data(data)
                .build());

            latest.setEvents(events);
            latest.setUpdatedAt(Instant.now());
            repository.save(latest);
        } catch (Exception e) {
            log.debug("Failed to append operation event: {}", e.getMessage());
        }
    }

    public void completeSuccess(String operationId, String message, Map<String, Object> metadataPatch) {
        complete(operationId, STATUS_SUCCESS, message, null, null, metadataPatch);
    }

    public void completeFailure(String operationId, String message, Throwable error, Map<String, Object> metadataPatch) {
        String stack = error != null ? toStackTrace(error, 12000) : null;
        String errMsg = error != null ? error.getMessage() : null;
        complete(operationId, STATUS_FAILED, message, errMsg, stack, metadataPatch);
    }

    public void clearContext() {
        OperationLogContext.clear();
    }

    private void complete(
        String operationId,
        String status,
        String message,
        String error,
        String stack,
        Map<String, Object> metadataPatch
    ) {
        try {
            OperationLog latest = repository.findFirstByOperationIdOrderByStartedAtDesc(operationId)
                .orElse(null);
            if (latest == null) {
                return;
            }

            Instant end = Instant.now();
            latest.setStatus(status);
            latest.setMessage(message);
            latest.setError(error);
            latest.setStackTrace(stack);
            latest.setEndedAt(end);

            if (latest.getStartedAt() != null) {
                latest.setDurationMs(end.toEpochMilli() - latest.getStartedAt().toEpochMilli());
            }

            if (metadataPatch != null && !metadataPatch.isEmpty()) {
                if (latest.getMetadata() == null) {
                    latest.setMetadata(metadataPatch);
                } else {
                    latest.getMetadata().putAll(metadataPatch);
                }
            }

            latest.setUpdatedAt(end);
            repository.save(latest);
        } catch (Exception e) {
            log.debug("Failed to complete operation log: {}", e.getMessage());
        }
    }

    private static String toStackTrace(Throwable t, int maxChars) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "\n...truncated...";
    }

    public List<OperationLog> getLogsForOperation(String operationId) {
        return repository.findByOperationIdOrderByStartedAtAsc(operationId);
    }
}
