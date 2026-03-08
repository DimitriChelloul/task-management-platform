package org.dimitri.task.application.service;

import org.dimitri.task.domain.Task;

import java.util.List;
import java.util.UUID;

public class TaskAppService {

    public List<Task> listTasks() {
        Task sample = new Task(UUID.randomUUID(), "Sample task from TaskAppService");
        return List.of(sample);
    }
}
