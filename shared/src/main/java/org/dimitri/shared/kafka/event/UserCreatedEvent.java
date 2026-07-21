package org.dimitri.shared.kafka.event;

public record UserCreatedEvent(String userId, String email, String createdAt) {
}
