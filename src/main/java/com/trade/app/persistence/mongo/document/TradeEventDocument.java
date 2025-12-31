package com.trade.app.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "trade_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEventDocument {

    @Id
    private String id;

    private String type;

    private String payload;

    private Instant createdAt;
}
