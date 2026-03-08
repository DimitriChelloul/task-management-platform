Lab 1 — Solution (reference implementation)

This file provides a complete solution that instructors can reveal after students attempt the lab.

1) In-memory repository (task-infrastructure)

```java
package org.dimitri.task.infrastructure;

import org.dimitri.task.domain.Task;
import org.dimitri.task.domain.TaskRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTaskRepository implements TaskRepository {
    private final Map<UUID, Task> db = new ConcurrentHashMap<>();

    @Override
    public void save(Task t) { db.put(t.getId(), t); }

    @Override
    public List<Task> findAll() { return new ArrayList<>(db.values()); }

    @Override
    public Optional<Task> findById(UUID id) { return Optional.ofNullable(db.get(id)); }
}
```

2) Controller (task-api)

```java
package org.dimitri.task.controller;

import org.dimitri.task.application.service.TaskAppService;
import org.dimitri.task.domain.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskAppService service;

    public TaskController(TaskAppService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Task> create(@RequestBody CreateTaskRequest req) {
        Task t = service.createTask(req.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    public static class CreateTaskRequest { private String title; public String getTitle() { return title; } public void setTitle(String title) { this.title = title; } }
}
```

3) Service (task-application)

```java
package org.dimitri.task.application.service;

import org.dimitri.task.domain.Task;
import org.dimitri.task.domain.TaskRepository;

import java.util.List;
import java.util.UUID;

public class TaskAppService {
    private final TaskRepository repo;

    public TaskAppService(TaskRepository repo) { this.repo = repo; }

    public List<Task> listTasks() { return repo.findAll(); }

    public Task createTask(String title) {
        Task t = new Task(UUID.randomUUID(), title, false);
        repo.save(t);
        return t;
    }
}
```

4) Wiring (Spring configuration) — register InMemoryTaskRepository as bean

```java
@Configuration
public class TaskConfig {
    @Bean
    public TaskRepository taskRepository() {
        return new InMemoryTaskRepository();
    }

    @Bean
    public TaskAppService taskAppService(TaskRepository repo) {
        return new TaskAppService(repo);
    }
}
```

5) Unit test example (TaskAppService)

```java
@Test
void createTaskAddsToRepo() {
    TaskRepository repo = new InMemoryTaskRepository();
    TaskAppService svc = new TaskAppService(repo);

    Task t = svc.createTask("Sample");
    assertEquals(1, repo.findAll().size());
}
```

Commit these changes in a reference branch `lab1-solution` and publish for students to review after attempting the lab.
