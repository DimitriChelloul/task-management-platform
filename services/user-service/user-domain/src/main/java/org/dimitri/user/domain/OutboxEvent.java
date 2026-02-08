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
        int attempts,
        Instant nextAttemptAt,
        String lastError
) {
    public enum Status { PENDING, SENT, FAILED }
}

