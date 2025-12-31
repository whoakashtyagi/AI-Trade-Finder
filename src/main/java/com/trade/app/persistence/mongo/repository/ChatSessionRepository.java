package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for AI chat sessions.
 * 
 * @author AI Trade Finder Team
 */
@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    List<ChatSession> findByTradeIdOrderByCreatedAtDesc(String tradeId);

    List<ChatSession> findBySymbolOrderByCreatedAtDesc(String symbol);

    List<ChatSession> findByTypeOrderByCreatedAtDesc(String type);

    List<ChatSession> findByActiveOrderByUpdatedAtDesc(boolean active);

    List<ChatSession> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    long countByTradeId(String tradeId);
}
