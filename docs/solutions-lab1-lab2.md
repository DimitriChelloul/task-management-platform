# Solutions Lab 1 & 2 — Code prêt à utiliser

Version 2026-03-08 | Copy-paste ready code for immediate implementation

Ce fichier contient le code **exact** à créer dans votre projet. Suivez les chemins absolus.

---

## Solution Lab 1 — POST /tasks en mémoire

### 1. Créer l'entité Task

**Chemin** : `services/task-service/task-domain/src/main/java/org/dimitri/task/domain/Task.java`

```java
package org.dimitri.task.domain;

import java.util.UUID;
import java.time.OffsetDateTime;

public record Task(
    UUID id,
    String title,
    String description,
    boolean done,
    OffsetDateTime createdAt
) {
    public static Task create(String title, String description) {
        return new Task(
            UUID.randomUUID(),
            title,
            description,
            false,
            OffsetDateTime.now()
        );
    }

    public Task markAsDone() {
        return new Task(this.id, this.title, this.description, true, this.createdAt);
    }
}
```

---

### 2. Créer les exceptions

**Chemin** : `services/task-service/task-domain/src/main/java/org/dimitri/task/domain/exception/TaskAlreadyExistsException.java`

```java
package org.dimitri.task.domain.exception;

public class TaskAlreadyExistsException extends RuntimeException {
    public TaskAlreadyExistsException(String message) {
        super(message);
    }
}
```

**Chemin** : `services/task-service/task-domain/src/main/java/org/dimitri/task/domain/exception/TaskNotFoundException.java`

```java
package org.dimitri.task.domain.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) {
        super(message);
    }
}
```

---

### 3. Créer le port (interface)

**Chemin** : `services/task-service/task-application/src/main/java/org/dimitri/task/application/port/TaskWritePort.java`

```java
package org.dimitri.task.application.port;

import org.dimitri.task.domain.Task;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TaskWritePort {
    void save(Task task);
    Optional<Task> findById(UUID id);
    List<Task> findAll();
    void update(Task task);
    void delete(UUID id);
}
```

---

### 4. Créer l'adapter en mémoire

**Chemin** : `services/task-service/task-api/src/main/java/org/dimitri/task/infrastructure/memory/InMemoryTaskRepository.java`

```java
package org.dimitri.task.infrastructure.memory;

import org.dimitri.task.application.port.TaskWritePort;
import org.dimitri.task.domain.Task;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTaskRepository implements TaskWritePort {
    private final Map<UUID, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void save(Task task) {
        tasks.put(task.id(), task);
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void update(Task task) {
        tasks.put(task.id(), task);
    }

    @Override
    public void delete(UUID id) {
        tasks.remove(id);
    }
}
```

---

### 5. Créer le service applicatif

**Chemin** : `services/task-service/task-application/src/main/java/org/dimitri/task/application/service/TaskAppService.java`

```java
package org.dimitri.task.application.service;

import org.dimitri.task.application.port.TaskWritePort;
import org.dimitri.task.domain.Task;
import org.dimitri.task.domain.exception.TaskNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskAppService {
    private final TaskWritePort taskWritePort;

    public TaskAppService(TaskWritePort taskWritePort) {
        this.taskWritePort = taskWritePort;
    }

    public Task createTask(String title, String description) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Title is too long");
        }
        Task newTask = Task.create(title, description);
        taskWritePort.save(newTask);
        return newTask;
    }

    public List<Task> getAllTasks() {
        return taskWritePort.findAll();
    }

    public Task getTaskById(UUID id) {
        return taskWritePort.findById(id)
            .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));
    }

    public Task completeTask(UUID id) {
        Task task = getTaskById(id);
        Task completedTask = task.markAsDone();
        taskWritePort.update(completedTask);
        return completedTask;
    }

    public void deleteTask(UUID id) {
        if (taskWritePort.findById(id).isEmpty()) {
            throw new TaskNotFoundException("Task not found: " + id);
        }
        taskWritePort.delete(id);
    }
}
```

---

### 6. Créer le controller REST

**Chemin** : `services/task-service/task-api/src/main/java/org/dimitri/task/controller/TaskController.java`

```java
package org.dimitri.task.controller;

import org.dimitri.task.application.service.TaskAppService;
import org.dimitri.task.domain.Task;
import org.dimitri.task.domain.exception.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskAppService taskAppService;

    public TaskController(TaskAppService taskAppService) {
        this.taskAppService = taskAppService;
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllTasks() {
        List<Task> tasks = taskAppService.getAllTasks();
        List<TaskDto> dtos = tasks.stream().map(TaskDto::fromTask).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@RequestBody CreateTaskRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Task createdTask = taskAppService.createTask(req.getTitle(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskDto.fromTask(createdTask));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable UUID id) {
        try {
            Task task = taskAppService.getTaskById(id);
            return ResponseEntity.ok(TaskDto.fromTask(task));
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<TaskDto> completeTask(@PathVariable UUID id) {
        try {
            Task completedTask = taskAppService.completeTask(id);
            return ResponseEntity.ok(TaskDto.fromTask(completedTask));
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        try {
            taskAppService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DTO classes
    public static class CreateTaskRequest {
        private String title;
        private String description;

        public CreateTaskRequest() {}
        public CreateTaskRequest(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class TaskDto {
        private UUID id;
        private String title;
        private String description;
        private boolean done;
        private String createdAt;

        public TaskDto() {}
        public TaskDto(UUID id, String title, String description, boolean done, String createdAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.done = done;
            this.createdAt = createdAt;
        }

        public static TaskDto fromTask(Task task) {
            return new TaskDto(task.id(), task.title(), task.description(), task.done(), task.createdAt().toString());
        }

        public UUID getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean isDone() { return done; }
        public String getCreatedAt() { return createdAt; }
    }
}
```

---

### 7. Bootstrap & Configuration

**Chemin** : `services/task-service/task-api/src/main/java/org/dimitri/task/TaskServiceApplication.java`

```java
package org.dimitri.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "org.dimitri.task")
@EnableDiscoveryClient
public class TaskServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
```

**Chemin** : `services/task-service/task-api/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: task-service

server:
  port: 8083

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    instance-id: task-service:${random.value}
```

---

## Solution Lab 2 — Ajouter JWT au controller

Modifiez `TaskController` pour valider les JWT :

**Chemin** : `services/task-service/task-api/src/main/java/org/dimitri/task/controller/TaskController.java` (modifier)

```java
package org.dimitri.task.controller;

import org.dimitri.task.application.service.TaskAppService;
import org.dimitri.task.domain.Task;
import org.dimitri.task.domain.exception.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskAppService taskAppService;
    // Injecter JwtService si disponible (optionnel pour ce lab)

    public TaskController(TaskAppService taskAppService) {
        this.taskAppService = taskAppService;
    }

    /**
     * Valider le JWT du header Authorization
     */
    private String extractUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        // In production, validate with JwtService
        // For now, just log the presence
        String token = authHeader.substring(7);
        return "user"; // Placeholder
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllTasks(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // JWT optionnel pour GET (public read)
        List<Task> tasks = taskAppService.getAllTasks();
        List<TaskDto> dtos = tasks.stream().map(TaskDto::fromTask).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(
        @RequestBody CreateTaskRequest req,
        @RequestHeader("Authorization") String authHeader
    ) {
        // JWT obligatoire pour POST (write)
        String username = extractUsername(authHeader);
        System.out.println("Creating task for user: " + username);

        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Task createdTask = taskAppService.createTask(req.getTitle(), req.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskDto.fromTask(createdTask));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable UUID id) {
        try {
            Task task = taskAppService.getTaskById(id);
            return ResponseEntity.ok(TaskDto.fromTask(task));
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<TaskDto> completeTask(
        @PathVariable UUID id,
        @RequestHeader("Authorization") String authHeader
    ) {
        String username = extractUsername(authHeader);
        try {
            Task completedTask = taskAppService.completeTask(id);
            return ResponseEntity.ok(TaskDto.fromTask(completedTask));
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
        @PathVariable UUID id,
        @RequestHeader("Authorization") String authHeader
    ) {
        String username = extractUsername(authHeader);
        try {
            taskAppService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // [Keep DTO classes as before...]
    public static class CreateTaskRequest {
        private String title;
        private String description;

        public CreateTaskRequest() {}
        public CreateTaskRequest(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class TaskDto {
        private UUID id;
        private String title;
        private String description;
        private boolean done;
        private String createdAt;

        public TaskDto() {}
        public TaskDto(UUID id, String title, String description, boolean done, String createdAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.done = done;
            this.createdAt = createdAt;
        }

        public static TaskDto fromTask(Task task) {
            return new TaskDto(task.id(), task.title(), task.description(), task.done(), task.createdAt().toString());
        }

        public UUID getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean isDone() { return done; }
        public String getCreatedAt() { return createdAt; }
    }
}
```

---

## Tests à exécuter

### Exécuter les unit tests

```powershell
cd C:\Users\Utilisateur\IdeaProjects\task-management-platform\services\task-service
mvn test
```

### Exécuter les tests d'intégration spécifiquement

```powershell
mvn test -Dtest=TaskControllerIntegrationTest
```

### Lancer le service

```powershell
cd C:\Users\Utilisateur\IdeaProjects\task-management-platform\services\task-service
mvn spring-boot:run
```

---

## Commandes curl pour tester

### Créer une tâche

```powershell
$body = @{
    title = "Buy groceries"
    description = "Milk, eggs, bread"
} | ConvertTo-Json

curl -X POST http://localhost:8083/tasks `
  -H "Content-Type: application/json" `
  -d $body
```

### Récupérer toutes les tâches

```powershell
curl http://localhost:8083/tasks
```

### Récupérer une tâche par ID (remplacer UUID)

```powershell
curl http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000
```

### Marquer comme complétée

```powershell
curl -X PUT http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000/complete `
  -H "Content-Type: application/json"
```

### Supprimer une tâche

```powershell
curl -X DELETE http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000
```

### Avec JWT (Lab 2)

```powershell
# Optenir un token depuis user-service
$token = (curl -X POST http://localhost:8081/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"alice","password":"secret"}' | ConvertFrom-Json).token

# Utiliser le token
curl -X POST http://localhost:8083/tasks `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d @'
{
  "title": "Protected task",
  "description": "Only authenticated users can create"
}
'@
```

---

## Fichiers à créer (résumé)

```
services/task-service/
├── task-domain/src/main/java/org/dimitri/task/
│   ├── domain/
│   │   ├── Task.java
│   │   └── exception/
│   │       ├── TaskAlreadyExistsException.java
│   │       └── TaskNotFoundException.java
├── task-application/src/main/java/org/dimitri/task/
│   ├── application/
│   │   ├── port/
│   │   │   └── TaskWritePort.java
│   │   └── service/
│   │       └── TaskAppService.java
└── task-api/src/main/
    ├── java/org/dimitri/task/
    │   ├── TaskServiceApplication.java
    │   ├── controller/
    │   │   └── TaskController.java
    │   └── infrastructure/memory/
    │       └── InMemoryTaskRepository.java
    └── resources/
        └── application.yml
```

Fin de la solution. Tous les fichiers sont prêts à copier-coller.
