package org.dimitri.user.api.controller;

import org.dimitri.user.api.events.UserCreatedEvent;
import org.dimitri.user.api.kafka.UserEventProducer;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/kafka")
public class KafkaTestController {

    private final UserEventProducer producer;

    public KafkaTestController(UserEventProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/user-created")
    public Map<String, String> publish(
            @RequestParam("userId") String userId,
            @RequestParam("email") String email
    ) {
        producer.publishUserCreated(new UserCreatedEvent(userId, email, Instant.now().toString()));
        return Map.of("status", "sent");
    }

}
