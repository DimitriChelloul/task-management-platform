package usecase;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import ports.OutboxWritePort;
import ports.UserWritePort;
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

        SecurityProperties.User user = new User(userId, email, now);

        // payload minimal JSON (simple et efficace)
        String payload = """
          {"userId":"%s","email":"%s","createdAt":"%s"}
        """.formatted(userId, email, now.toString()).trim();

        OutboxEvent evt = new OutboxEvent(
                UUID.randomUUID(),
                "User",
                userId,
                "UserCreated",
                payload,
                "PENDING",
                now
        );

        userWritePort.save(user);
        outboxWritePort.save(evt);

        return userId;
    }
}

