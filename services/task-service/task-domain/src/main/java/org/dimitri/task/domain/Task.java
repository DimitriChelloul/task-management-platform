package org.dimitri.task.domain;

import java.time.Instant;
import java.util.UUID;

public record Task(
        UUID id,
        String title,
        Status status,
        String sourceUserId,
        Instant createdAt,
        Instant completedAt
) {
    public enum Status { OPEN, COMPLETED }
}
