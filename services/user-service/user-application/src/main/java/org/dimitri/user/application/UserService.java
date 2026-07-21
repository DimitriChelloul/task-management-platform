package org.dimitri.user.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dimitri.shared.kafka.KafkaEventTypes;
import org.dimitri.shared.kafka.event.UserCreatedEvent;
import org.dimitri.shared.outbox.OutboxEvent;
import org.dimitri.shared.outbox.OutboxPort;
import org.dimitri.user.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class UserService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository users;
    private final OutboxPort outbox;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository users, OutboxPort outbox, ObjectMapper objectMapper) {
        this.users = users;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public User create(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (users.findByEmail(email).isPresent()) throw new EmailAlreadyUsedException(email);

        Instant createdAt = Instant.now();
        User user = new User(UUID.randomUUID(), email, createdAt);
        users.save(user);
        appendUserCreated(user);
        return user;
    }

    public List<User> list() {
        return users.findAll();
    }

    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException(id.toString()));
    }

    public User authenticate(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        return users.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    private void appendUserCreated(User user) {
        UserCreatedEvent event = new UserCreatedEvent(
                user.id().toString(), user.email(), user.createdAt().toString());
        try {
            outbox.save(new OutboxEvent(UUID.randomUUID(), "USER", user.id().toString(),
                    KafkaEventTypes.USER_CREATED, objectMapper.writeValueAsString(event),
                    OutboxEvent.Status.PENDING, user.createdAt(), 0, user.createdAt(), null));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize user-created event", exception);
        }
    }

    private static String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) throw new IllegalArgumentException("Invalid email");
        return normalized;
    }
}
