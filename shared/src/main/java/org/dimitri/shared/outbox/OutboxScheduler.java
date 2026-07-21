package org.dimitri.shared.outbox;

import org.dimitri.shared.kafka.KafkaEventPublisher;
import org.dimitri.shared.kafka.KafkaTopicsProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxScheduler {
    private final OutboxPort outbox;
    private final KafkaEventPublisher publisher;
    private final KafkaTopicsProperties topics;
    private final int batchSize;

    public OutboxScheduler(OutboxPort outbox, KafkaEventPublisher publisher, KafkaTopicsProperties topics,
                           @Value("${outbox.batch-size:50}") int batchSize) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.topics = topics;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval:2s}")
    public void publishPending() {
        for (OutboxEvent event : outbox.findPending(batchSize)) {
            try {
                publisher.publish(topics.forEventType(event.eventType()), event.aggregateId(), event.payload());
                outbox.markSent(event.id());
            } catch (Exception exception) {
                outbox.markFailed(event.id(), exception.getMessage());
            }
        }
    }
}
