package org.dimitri.shared.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OutboxRetryPolicy {
    private final Duration initialDelay;
    private final Duration maximumDelay;

    public OutboxRetryPolicy(
            @Value("${outbox.retry.initial-delay:5s}") Duration initialDelay,
            @Value("${outbox.retry.maximum-delay:5m}") Duration maximumDelay) {
        this.initialDelay = initialDelay;
        this.maximumDelay = maximumDelay;
    }

    public Duration delayForAttempt(int attempt) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 20);
        return Duration.ofMillis(Math.min(maximumDelay.toMillis(), initialDelay.toMillis() * multiplier));
    }
}
