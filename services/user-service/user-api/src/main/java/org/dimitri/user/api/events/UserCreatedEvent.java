package org.dimitri.user.api.events;

public record UserCreatedEvent(
        String userId,
        String email,
        String createdAt
) {}
