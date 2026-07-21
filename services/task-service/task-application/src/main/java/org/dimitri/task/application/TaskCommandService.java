package org.dimitri.task.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dimitri.shared.kafka.KafkaEventTypes;
import org.dimitri.shared.kafka.event.TaskCompletedEvent;
import org.dimitri.shared.kafka.event.TaskCreatedEvent;
import org.dimitri.shared.outbox.OutboxEvent;
import org.dimitri.shared.outbox.OutboxPort;
import org.dimitri.task.domain.Task;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TaskCommandService {
    private final TaskRepository tasks;
    private final OutboxPort outbox;
    private final ObjectMapper objectMapper;

    public TaskCommandService(TaskRepository tasks, OutboxPort outbox, ObjectMapper objectMapper) {
        this.tasks = tasks;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Task create(String title) {
        return create(title, null);
    }

    @Transactional
    public void createWelcomeTask(String userId) {
        create("Welcome task", userId);
    }

    public List<Task> list() {
        return tasks.findAll();
    }

    @Transactional
    public Task complete(UUID id) {
        Task task = tasks.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        if (task.status() == Task.Status.COMPLETED) return task;

        Instant completedAt = Instant.now();
        if (!tasks.markCompleted(id, completedAt)) {
            return tasks.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        }
        appendEvent(id, KafkaEventTypes.TASK_COMPLETED,
                new TaskCompletedEvent(id.toString(), completedAt.toString()), completedAt);
        return new Task(id, task.title(), Task.Status.COMPLETED, task.sourceUserId(), task.createdAt(), completedAt);
    }

    private Task create(String title, String sourceUserId) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Task title is required");
        Instant createdAt = Instant.now();
        Task task = new Task(UUID.randomUUID(), title.trim(), Task.Status.OPEN, sourceUserId, createdAt, null);
        if (tasks.save(task)) {
            appendEvent(task.id(), KafkaEventTypes.TASK_CREATED,
                    new TaskCreatedEvent(task.id().toString(), task.title(), createdAt.toString()), createdAt);
        }
        return task;
    }

    private void appendEvent(UUID aggregateId, String eventType, Object payload, Instant createdAt) {
        try {
            outbox.save(new OutboxEvent(UUID.randomUUID(), "TASK", aggregateId.toString(), eventType,
                    objectMapper.writeValueAsString(payload), OutboxEvent.Status.PENDING,
                    createdAt, 0, createdAt, null));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize task event", exception);
        }
    }
}
