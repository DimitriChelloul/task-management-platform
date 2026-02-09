package org.dimitri.user.infrastructure.memory;

import org.dimitri.user.application.ports.UserWritePort;
import org.dimitri.user.domain.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class InMemoryUserWriteAdapter implements UserWritePort {

    private final Map<UUID, User> store = new ConcurrentHashMap<>();

    @Override
    public void save(User user) {
        store.put(user.id(), user);
    }
}

