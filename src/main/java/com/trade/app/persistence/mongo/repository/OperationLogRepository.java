package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperationLogRepository extends MongoRepository<OperationLog, String> {

    List<OperationLog> findByOperationIdOrderByStartedAtAsc(String operationId);

    Optional<OperationLog> findFirstByOperationIdOrderByStartedAtDesc(String operationId);

    Page<OperationLog> findByOperationTypeOrderByStartedAtDesc(String operationType, Pageable pageable);

    Page<OperationLog> findByStatusOrderByStartedAtDesc(String status, Pageable pageable);

    Page<OperationLog> findBySourceOrderByStartedAtDesc(String source, Pageable pageable);

    Page<OperationLog> findByStartedAtBetweenOrderByStartedAtDesc(Instant from, Instant to, Pageable pageable);
}
