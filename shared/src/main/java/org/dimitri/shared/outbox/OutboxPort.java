package org.dimitri.shared.outbox;

import java.util.List;
import java.util.UUID;

public interface OutboxPort {
    void save(OutboxEvent event);
    List<OutboxEvent> findPending(int limit);
    void markSent(UUID eventId);
    void markFailed(UUID eventId, String error);
}
