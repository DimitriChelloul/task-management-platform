package org.dimitri.task.controller;

import org.dimitri.task.application.TaskCommandService;
import org.dimitri.task.domain.Task;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskCommandService tasks;

    public TaskController(TaskCommandService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    public List<Task> list() {
        return tasks.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@RequestBody CreateTaskRequest request) {
        return tasks.create(request.title());
    }

    @PatchMapping("/{id}/complete")
    public Task complete(@PathVariable UUID id) {
        return tasks.complete(id);
    }

    public record CreateTaskRequest(String title) {
    }
}
