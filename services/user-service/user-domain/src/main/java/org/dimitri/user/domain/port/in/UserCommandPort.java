package org.dimitri.user.domain.port.in;


import java.util.UUID;


public interface UserCommandPort {
    UUID createUser(String email);
}


