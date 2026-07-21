package org.dimitri.shared.kafka.event;

public record TaskCompletedEvent(String taskId, String completedAt) {
}
