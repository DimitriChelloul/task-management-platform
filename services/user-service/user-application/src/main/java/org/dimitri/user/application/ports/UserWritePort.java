package org.dimitri.user.application.ports;

import org.dimitri.user.domain.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWritePort {
    void save(User user);
}
