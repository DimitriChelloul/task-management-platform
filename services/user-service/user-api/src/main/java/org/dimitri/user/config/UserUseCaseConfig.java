package org.dimitri.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dimitri.user.application.UserRepository;
import org.dimitri.user.application.UserService;
import org.dimitri.shared.outbox.OutboxPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserUseCaseConfig {

    @Bean
    public UserService userService(UserRepository users, OutboxPort outbox, ObjectMapper objectMapper) {
        return new UserService(users, outbox, objectMapper);
    }
}
