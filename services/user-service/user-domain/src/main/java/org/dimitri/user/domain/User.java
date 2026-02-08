package org.dimitri.user.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        Instant createdAt
) {}
