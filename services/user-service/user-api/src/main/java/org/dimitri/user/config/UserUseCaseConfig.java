package org.dimitri.user.config;

import org.dimitri.user.application.usecase.CreateUserUseCase;
import org.dimitri.user.application.ports.UserWritePort;
import org.dimitri.user.application.ports.OutboxWritePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserUseCaseConfig {

    @Bean
    public CreateUserUseCase createUserUseCase(UserWritePort userWritePort,
                                               OutboxWritePort outboxWritePort) {
        return new CreateUserUseCase(userWritePort, outboxWritePort);
    }
}
