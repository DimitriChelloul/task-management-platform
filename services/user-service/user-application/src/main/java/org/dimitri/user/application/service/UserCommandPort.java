package org.dimitri.user.application.service;

import java.util.UUID;

public interface UserCommandPort {

    public UUID createUser(String email);
}
