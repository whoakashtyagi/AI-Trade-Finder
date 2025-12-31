package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.SchedulerConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for dynamic scheduler configurations.
 * 
 * @author AI Trade Finder Team
 */
@Repository
public interface SchedulerConfigRepository extends MongoRepository<SchedulerConfig, String> {

    Optional<SchedulerConfig> findByName(String name);

    List<SchedulerConfig> findByEnabled(boolean enabled);

    List<SchedulerConfig> findByType(String type);

    List<SchedulerConfig> findByEnabledAndType(boolean enabled, String type);

    boolean existsByName(String name);
}
