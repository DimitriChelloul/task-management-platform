package org.dimitri.shared.kafka;

public final class KafkaEventTypes {
    public static final String USER_CREATED = "USER_CREATED";
    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_COMPLETED = "TASK_COMPLETED";

    private KafkaEventTypes() {
    }
}
