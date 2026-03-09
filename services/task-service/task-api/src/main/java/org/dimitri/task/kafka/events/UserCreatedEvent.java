package org.dimitri.task.kafka.events;

public record UserCreatedEvent(
        String userId,
        String email,
        String createdAt
) {
}
