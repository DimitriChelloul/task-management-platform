package org.dimitri.user.application.service;



import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserAppService implements org.dimitri.user.application.service.UserCommandPort {

    @Override
    public UUID createUser(String email) {
        return UUID.randomUUID();
    }
}

