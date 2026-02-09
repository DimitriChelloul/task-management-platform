package org.dimitri.user.infrastructure.persistence;

import org.dimitri.user.domain.User;

public interface UserRepository {
    void insert(User user);
}
