package org.dimitri.shared.kafka.event;

public record TaskCreatedEvent(String taskId, String title, String createdAt) {
}
