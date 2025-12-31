package com.trade.app.persistence.mongo.repository;

import com.trade.app.persistence.mongo.document.TradeEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TradeEventRepository extends MongoRepository<TradeEventDocument, String> {
}
