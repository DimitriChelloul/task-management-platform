package org.dimitri.user.infrastructure.memory;

import org.dimitri.user.application.ports.OutboxWritePort;
import org.dimitri.user.domain.OutboxEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("dev")
public class InMemoryOutboxWriteAdapter implements OutboxWritePort {

    private final List<OutboxEvent> outbox = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
        outbox.add(event);
        // option: log event
        System.out.println("[OUTBOX] saved event=" + event.eventType() + " id=" + event.id());
    }
}

