package ports;

import org.dimitri.user.domain.OutboxEvent;
import java.util.List;

public interface OutboxReadPort {
    List<OutboxEvent> findPending(int limit);
    void markSent(java.util.UUID eventId);
    void markFailed(java.util.UUID eventId, String error);
}
