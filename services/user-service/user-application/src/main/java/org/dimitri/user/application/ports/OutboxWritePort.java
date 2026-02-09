package org.dimitri.user.application.ports;

import org.dimitri.user.domain.OutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxWritePort {
    void save(OutboxEvent event);
}
