package org.dimitri.task.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dimitri.task.kafka.events.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedEventListener.class);

    private final ObjectMapper objectMapper;

    public UserCreatedEventListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${kafka.topics.userCreated:user.created}",
            groupId = "${spring.kafka.consumer.group-id:task-service-group}"
    )
    public void onUserCreated(String payload) {
        try {
            UserCreatedEvent event = objectMapper.readValue(payload, UserCreatedEvent.class);
            log.info("Received user.created event for userId={} email={}", event.userId(), event.email());
        } catch (Exception e) {
            log.error("Failed to process user.created payload={}", payload, e);
        }
    }
}
