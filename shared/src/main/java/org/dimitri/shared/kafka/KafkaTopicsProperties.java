package org.dimitri.shared.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(String userCreated, String taskCreated, String taskCompleted) {
    public KafkaTopicsProperties {
        if (userCreated == null || userCreated.isBlank()) userCreated = "user.created";
        if (taskCreated == null || taskCreated.isBlank()) taskCreated = "task.created";
        if (taskCompleted == null || taskCompleted.isBlank()) taskCompleted = "task.completed";
    }

    public String forEventType(String eventType) {
        return switch (eventType) {
            case KafkaEventTypes.USER_CREATED -> userCreated;
            case KafkaEventTypes.TASK_CREATED -> taskCreated;
            case KafkaEventTypes.TASK_COMPLETED -> taskCompleted;
            default -> throw new IllegalArgumentException("Unknown Kafka event type: " + eventType);
        };
    }
}
