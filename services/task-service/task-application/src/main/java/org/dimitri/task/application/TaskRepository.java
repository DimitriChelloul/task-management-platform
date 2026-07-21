package org.dimitri.task.application;

import org.dimitri.task.domain.Task;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    boolean save(Task task);
    List<Task> findAll();
    Optional<Task> findById(UUID id);
    boolean markCompleted(UUID id, Instant completedAt);
}
