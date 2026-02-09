package org.dimitri.user.api.outbox;



import org.dimitri.user.application.ports.OutboxReadPort;
import org.dimitri.user.domain.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxScheduler {

    private final OutboxReadPort outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxScheduler(OutboxReadPort outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:2000}")
    public void publishPending() {
        for (OutboxEvent evt : outbox.findPending(50)) {
            try {
                // Topic = user.created (tu peux mapper eventType -> topic si tu veux)
                kafka.send("user.created", evt.aggregateId().toString(), evt.payloadJson());
                outbox.markSent(evt.id());
            } catch (Exception e) {
                outbox.markFailed(evt.id(), e.getMessage());
            }
        }
    }
}

