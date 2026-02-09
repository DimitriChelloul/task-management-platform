package org.dimitri.user.api.adapters.in;

import org.dimitri.user.domain.port.in.UserCommandPort;
import org.dimitri.user.application.usecase.CreateUserUseCase;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Component
public class UserCommandAdapter implements UserCommandPort {

    private final CreateUserUseCase createUserUseCase;

    public UserCommandAdapter(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    @Override
    public UUID createUser(String email) {
        return createUserUseCase.handle(email);
    }
}

