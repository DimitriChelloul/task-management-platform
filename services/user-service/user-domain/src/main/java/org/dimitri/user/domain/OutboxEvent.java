package org.dimitri.user.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        Status status,
        Instant createdAt,
        int retryCount,
        Instant nextAttemptAt,
        String lastError
) {
    public String payloadJson() {
        return "";
    }

    public Object attempts() {
        return null;
    }

    public enum Status { PENDING, SENT, FAILED }

    public static OutboxEvent create(
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload
    ) {
        Instant now = Instant.now();
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                payload,
                Status.PENDING,
                now,
                0,
                now,     // ou now.plusSeconds(5) si tu veux un délai
                null
        );
    }
}
