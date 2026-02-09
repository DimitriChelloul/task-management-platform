package org.dimitri.user.application.usecase;

import org.dimitri.user.application.ports.OutboxWritePort;
import org.dimitri.user.application.ports.UserWritePort;
import org.dimitri.user.domain.OutboxEvent;
import org.dimitri.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public class CreateUserUseCase {

    private final UserWritePort userWritePort;
    private final OutboxWritePort outboxWritePort;
    

    public CreateUserUseCase(UserWritePort userWritePort, OutboxWritePort outboxWritePort) {
        this.userWritePort = userWritePort;
        this.outboxWritePort = outboxWritePort;
    }

    public UUID handle(String email) {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        User user = new User(userId, email, now);

        // payload minimal JSON (simple et efficace)
        String payload = """
          {"userId":"%s","email":"%s","createdAt":"%s"}
        """.formatted(userId, email, now.toString()).trim();

        String payloadJson = """
  {"userId":"%s","email":"%s","createdAt":"%s"}
""".formatted(userId, email, now.toString()).trim();

        OutboxEvent evt = new OutboxEvent(
                UUID.randomUUID(),
                "USER",
                userId.toString(),
                "USER_CREATED",
                payloadJson,
                OutboxEvent.Status.PENDING,
                now,
                0,
                now,
                null
        );


        userWritePort.save(user);
        outboxWritePort.save(evt);

        return userId;
    }
}

