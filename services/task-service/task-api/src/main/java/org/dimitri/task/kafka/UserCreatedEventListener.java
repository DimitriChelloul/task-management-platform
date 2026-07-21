package org.dimitri.task.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dimitri.shared.kafka.event.UserCreatedEvent;
import org.dimitri.task.application.TaskCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedEventListener.class);

    private final ObjectMapper objectMapper;
    private final TaskCommandService tasks;

    public UserCreatedEventListener(ObjectMapper objectMapper, TaskCommandService tasks) {
        this.objectMapper = objectMapper;
        this.tasks = tasks;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.user-created:user.created}",
            groupId = "${spring.kafka.consumer.group-id:task-service-group}"
    )
    public void onUserCreated(String payload) throws JsonProcessingException {
        UserCreatedEvent event = objectMapper.readValue(payload, UserCreatedEvent.class);
        tasks.createWelcomeTask(event.userId());
        log.info("Received user.created event for userId={} email={}", event.userId(), event.email());
    }
}
