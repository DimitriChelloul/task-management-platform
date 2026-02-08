package persistence;

import org.dimitri.user.domain.OutboxEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository {
    void insert(OutboxEvent event);
    List<OutboxEvent> findPending(int limit);
    void markSent(UUID id);
    void markFailed(UUID id, String error);

    // Optionnel (utile pour debug / tests)
    Optional<OutboxEvent> findById(UUID id);
}
