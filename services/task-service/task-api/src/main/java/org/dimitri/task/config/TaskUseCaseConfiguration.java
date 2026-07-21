package org.dimitri.task.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dimitri.shared.outbox.OutboxPort;
import org.dimitri.task.application.TaskCommandService;
import org.dimitri.task.application.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskUseCaseConfiguration {
    @Bean
    TaskCommandService taskCommandService(TaskRepository tasks, OutboxPort outbox, ObjectMapper objectMapper) {
        return new TaskCommandService(tasks, outbox, objectMapper);
    }
}
