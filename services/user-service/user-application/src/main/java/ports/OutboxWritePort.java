package ports;

import org.dimitri.user.domain.OutboxEvent;

public interface OutboxWritePort {
    void save(OutboxEvent event);
}
