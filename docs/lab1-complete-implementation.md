# Lab 1 — Implémentation complète : POST /tasks en mémoire

Version 2026-03-08 | Exercice guidé pour créer un microservice Task complet

---

## Objectif

Implémenter un endpoint **POST /tasks** en suivant l'architecture hexagonale, sans modifier le code existant (user-service). À la fin du lab, vous saurez :

1. Créer une entité domaine immuable.
2. Définir des ports (interfaces).
3. Implémenter des adapters (en mémoire d'abord).
4. Orchestrer via un service applicatif.
5. Exposer un controller REST.
6. Écrire des tests complets.

---

## Étape 1 — Créer la classe Task (domaine)

**Fichier** : `services/task-service/task-domain/src/main/java/org/dimitri/task/domain/Task.java`

```java
package org.dimitri.task.domain;

import java.util.UUID;
import java.time.OffsetDateTime;

/**
 * Entité Task — immuable, représente une tâche à faire.
 * 
 * Un record génère automatiquement :
 * - Constructor complet
 * - Getters (pas de prefix 'get', ex: id() au lieu de getId())
 * - equals(), hashCode(), toString()
 */
public record Task(
    UUID id,              // Identifiant unique
    String title,         // Description courte
    String description,   // Détails (optionnel)
    boolean done,         // État : vrai si complétée
    OffsetDateTime createdAt  // Timestamp de création
) {
    /**
     * Factory method — créer une nouvelle Task avec ID généré automatiquement.
     * 
     * @param title la description courte de la tâche
     * @param description détails complets
     * @return une nouvelle Task avec ID UUID et createdAt = maintenant
     */
    public static Task create(String title, String description) {
        return new Task(
            UUID.randomUUID(),       // Génère un UUID aléatoire
            title,
            description,
            false,                   // Initialement non-complétée
            OffsetDateTime.now()     // Timestamp actuel avec timezone
        );
    }

    /**
     * Factory method pour marquer une tâche comme complétée.
     * 
     * @return une nouvelle Task avec done=true (record immuable)
     */
    public Task markAsDone() {
        return new Task(this.id, this.title, this.description, true, this.createdAt);
    }
}
```

**Explications** :
- `record` = classe immuable générée automatiquement (Java 16+).
- `UUID.randomUUID()` : identifiant unique sans collision (préféré aux auto-increment en microservices).
- `OffsetDateTime` : timestamp avec timezone (ISO 8601 standard).
- Factory methods (`create()`, `markAsDone()`) pour créer des instances.

---

## Étape 2 — Créer les ports (interfaces métier)

**Fichier** : `services/task-service/task-application/src/main/java/org/dimitri/task/application/ports/TaskWritePort.java`

```java
package org.dimitri.task.application.ports;

import org.dimitri.task.domain.Task;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Port (interface) — définit les opérations de persistence attendues.
 * 
 * Le domaine utilise ce port ; il ne sait PAS si l'implémentation
 * est en mémoire, JDBC, JPA, ou une API REST.
 * 
 * Bénéfices :
 * - Testabilité : on peut mocker ce port en tests
 * - Flexibilité : changer l'implémentation sans toucher au domaine
 * - Clarté : le domaine expose explicitement ses besoins
 */
public interface TaskWritePort {

    /**
     * Persister une nouvelle tâche.
     * 
     * @param task l'entité Task (avec ID généré)
     * @throws TaskAlreadyExistsException si la tâche existe déjà
     */
    void save(Task task) throws TaskAlreadyExistsException;

    /**
     * Récupérer une tâche par son ID.
     * 
     * @param id l'UUID
     * @return Optional avec la Task si trouvée, vide sinon
     */
    Optional<Task> findById(UUID id);

    /**
     * Récupérer toutes les tâches.
     * 
     * @return liste (potentiellement vide)
     */
    List<Task> findAll();

    /**
     * Mettre à jour une tâche existante.
     * 
     * @param task l'entité modifiée
     * @throws TaskNotFoundException si la tâche n'existe pas
     */
    void update(Task task) throws TaskNotFoundException;

    /**
     * Supprimer une tâche.
     * 
     * @param id l'UUID
     */
    void delete(UUID id);
}
```

**Explications** :
- Port = interface pure, aucune implémentation.
- Défini en couche `application` pour que le domaine l'utilise.
- Les adapters (couche `infrastructure`) implémentent ce port.

**Fichier** : `services/task-service/task-application/src/main/java/org/dimitri/task/application/exception/TaskAlreadyExistsException.java`

```java
package org.dimitri.task.application.exception;

/**
 * Exception levée quand on tente de créer une tâche avec un ID déjà existant.
 */
public class TaskAlreadyExistsException extends RuntimeException {
    public TaskAlreadyExistsException(String message) {
        super(message);
    }
}
```

**Fichier** : `services/task-service/task-application/src/main/java/org/dimitri/task/application/exception/TaskNotFoundException.java`

```java
package org.dimitri.task.application.exception;

/**
 * Exception levée quand on cherche une tâche qui n'existe pas.
 */
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) {
        super(message);
    }
}
```

---

## Étape 3 — Implémenter un adapter en mémoire (Map)

**Fichier** : `services/task-service/task-api/src/main/java/org/dimitri/task/infrastructure/memory/InMemoryTaskRepository.java`

```java
package org.dimitri.task.infrastructure.memory;

import org.dimitri.task.application.ports.TaskWritePort;
import org.dimitri.task.application.exception.TaskAlreadyExistsException;
import org.dimitri.task.application.exception.TaskNotFoundException;
import org.dimitri.task.domain.Task;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter en mémoire — implémente TaskWritePort en utilisant une Map.
 * 
 * Avantages :
 * - Simple, aucune dépendance externe
 * - Rapide pour les tests et développement
 * 
 * Inconvénients :
 * - Données perdues au redémarrage du service
 * - Pas persistant
 * - Limité à une seule instance (pas de scaling horizontal)
 * 
 * Usage : idéal pour le Lab 1 et les tests unitaires.
 */
@Repository
public class InMemoryTaskRepository implements TaskWritePort {

    /**
     * ConcurrentHashMap : thread-safe, pas de synchronisation manuelle.
     * Clé = UUID, Valeur = Task
     */
    private final Map<UUID, Task> tasks = new ConcurrentHashMap<>();

    @Override
    public void save(Task task) throws TaskAlreadyExistsException {
        if (tasks.containsKey(task.id())) {
            throw new TaskAlreadyExistsException("Task with ID " + task.id() + " already exists");
        }
        tasks.put(task.id(), task);
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());  // Retourner une copie pour éviter les modifications externes
    }

    @Override
    public void update(Task task) throws TaskNotFoundException {
        if (!tasks.containsKey(task.id())) {
            throw new TaskNotFoundException("Task with ID " + task.id() + " not found");
        }
        tasks.put(task.id(), task);  // Remplacer la tâche
    }

    @Override
    public void delete(UUID id) {
        tasks.remove(id);
    }
}
```

**Points clés** :
- `ConcurrentHashMap` : thread-safe, suitable pour une application multi-threaded (Spring gère ça).
- `save()` : vérifier l'unicité avant d'insérer.
- `findAll()` : retourner une **copie** de la liste (pas d'exposition interne).

---

## Étape 4 — Créer le service applicatif (orchestration)

**Fichier** : `services/task-service/task-application/src/main/java/org/dimitri/task/application/service/TaskAppService.java`

```java
package org.dimitri.task.application.service;

import org.dimitri.task.application.ports.TaskWritePort;
import org.dimitri.task.application.exception.TaskAlreadyExistsException;
import org.dimitri.task.application.exception.TaskNotFoundException;
import org.dimitri.task.domain.Task;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service applicatif — orchestre la logique d'application.
 * 
 * Rôles :
 * 1. Valider les entrées (business rules)
 * 2. Orchestrer les ports (appels à la persistence, à d'autres services)
 * 3. Gérer les transactions (si applicable)
 * 4. Retourner des DTOs appropriés au controller
 * 
 * Le service NE connaît PAS l'implémentation du port (peut être mémoire, JDBC, JPA, etc.)
 */
@Service
public class TaskAppService {

    private final TaskWritePort taskWritePort;

    /**
     * Injection de dépendance du port.
     * Spring choisira automatiquement l'implémentation disponible
     * (ici : InMemoryTaskRepository).
     */
    public TaskAppService(TaskWritePort taskWritePort) {
        this.taskWritePort = taskWritePort;
    }

    /**
     * Use-case : créer une nouvelle tâche.
     * 
     * @param title la description courte
     * @param description les détails
     * @return la tâche créée
     */
    public Task createTask(String title, String description) {
        // 1. Validation
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Title is too long (max 255 chars)");
        }

        // 2. Créer l'entité domaine
        Task newTask = Task.create(title, description);

        // 3. Persister via le port
        taskWritePort.save(newTask);

        // 4. Retourner l'entité créée
        return newTask;
    }

    /**
     * Use-case : récupérer toutes les tâches.
     */
    public List<Task> getAllTasks() {
        return taskWritePort.findAll();
    }

    /**
     * Use-case : récupérer une tâche par son ID.
     */
    public Task getTaskById(UUID id) {
        return taskWritePort.findById(id)
            .orElseThrow(() -> new TaskNotFoundException("Task not found: " + id));
    }

    /**
     * Use-case : marquer une tâche comme complétée.
     */
    public Task completeTask(UUID id) {
        Task task = getTaskById(id);  // Récupère ou lance TaskNotFoundException
        Task completedTask = task.markAsDone();  // Entité immuable : crée une nouvelle instance
        taskWritePort.update(completedTask);
        return completedTask;
    }

    /**
     * Use-case : supprimer une tâche.
     */
    public void deleteTask(UUID id) {
        if (taskWritePort.findById(id).isEmpty()) {
            throw new TaskNotFoundException("Task not found: " + id);
        }
        taskWritePort.delete(id);
    }
}
```

**Points clés** :
- Service injecte le port, pas l'implémentation.
- Valide les entrées (business rules).
- Orchestre les ports.
- Ne connaît pas les détails d'implémentation (JDBC, JPA, etc.)

---

## Étape 5 — Créer le controller REST

**Fichier** : `services/task-service/task-api/src/main/java/org/dimitri/task/controller/TaskController.java`

```java
package org.dimitri.task.controller;

import org.dimitri.task.application.service.TaskAppService;
import org.dimitri.task.application.exception.TaskNotFoundException;
import org.dimitri.task.domain.Task;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST — point d'entrée HTTP pour les opérations sur tâches.
 * 
 * Routes :
 * - GET  /tasks           → récupérer toutes les tâches
 * - POST /tasks           → créer une nouvelle tâche
 * - GET  /tasks/{id}      → récupérer une tâche par ID
 * - PUT  /tasks/{id}      → marquer comme complétée (ou update générique)
 * - DELETE /tasks/{id}    → supprimer une tâche
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskAppService taskAppService;

    public TaskController(TaskAppService taskAppService) {
        this.taskAppService = taskAppService;
    }

    /**
     * GET /tasks — récupérer toutes les tâches.
     * 
     * Example:
     *   curl http://localhost:8083/tasks
     */
    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllTasks() {
        List<Task> tasks = taskAppService.getAllTasks();
        List<TaskDto> dtos = tasks.stream()
            .map(TaskDto::fromTask)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /tasks — créer une nouvelle tâche.
     * 
     * Example:
     *   curl -X POST http://localhost:8083/tasks \
     *     -H "Content-Type: application/json" \
     *     -d '{"title": "Buy groceries", "description": "Milk, eggs, bread"}'
     */
    @PostMapping
    public ResponseEntity<TaskDto> createTask(@RequestBody CreateTaskRequest req) {
        // Validation de base
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Appeler le service
        Task createdTask = taskAppService.createTask(req.getTitle(), req.getDescription());

        // Retourner 201 Created avec la ressource créée
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(TaskDto.fromTask(createdTask));
    }

    /**
     * GET /tasks/{id} — récupérer une tâche par son ID.
     * 
     * Example:
     *   curl http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable UUID id) {
        try {
            Task task = taskAppService.getTaskById(id);
            return ResponseEntity.ok(TaskDto.fromTask(task));
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT /tasks/{id}/complete — marquer une tâche comme complétée.
     * 
     * Example:
     *   curl -X PUT http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000/complete
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<TaskDto> completeTask(@PathVariable UUID id) {
        try {
            Task completedTask = taskAppService.completeTask(id);
            return ResponseEntity.ok(TaskDto.fromTask(completedTask));
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /tasks/{id} — supprimer une tâche.
     * 
     * Example:
     *   curl -X DELETE http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        try {
            taskAppService.deleteTask(id);
            return ResponseEntity.noContent().build();  // 204 No Content
        } catch (TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ============ DTOs ============

    /**
     * DTO pour les requêtes POST /tasks.
     */
    public static class CreateTaskRequest {
        private String title;
        private String description;

        public CreateTaskRequest() {}

        public CreateTaskRequest(String title, String description) {
            this.title = title;
            this.description = description;
        }

        // Getters & Setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * DTO pour les réponses (GET, POST, PUT, etc.)
     */
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

        /**
         * Convertir une entité Task en DTO pour la réponse HTTP.
         */
        public static TaskDto fromTask(Task task) {
            return new TaskDto(
                task.id(),
                task.title(),
                task.description(),
                task.done(),
                task.createdAt().toString()
            );
        }

        // Getters (setters généralement omis pour les DTOs)
        public UUID getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean isDone() { return done; }
        public String getCreatedAt() { return createdAt; }
    }
}
```

---

## Étape 6 — Configuration Spring (wiring)

**Fichier** : `services/task-service/task-api/src/main/java/org/dimitri/task/config/TaskUseCaseConfig.java`

```java
package org.dimitri.task.config;

import org.dimitri.task.application.service.TaskAppService;
import org.dimitri.task.application.ports.TaskWritePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Spring — wiring des composants.
 * 
 * Bien que Spring peut auto-découvrir les beans via @Component, @Service, @Repository,
 * il est souvent utile d'avoir une configuration explicite pour la lisibilité
 * et le contrôle du DI (Dependency Injection).
 */
@Configuration
public class TaskUseCaseConfig {

    /**
     * Créer le bean TaskAppService.
     * Spring injectera automatiquement le TaskWritePort (implémenté par InMemoryTaskRepository).
     */
    @Bean
    public TaskAppService taskAppService(TaskWritePort taskWritePort) {
        return new TaskAppService(taskWritePort);
    }
}
```

---

## Étape 7 — Application principale (Bootstrap)

**Fichier** : `services/task-service/task-api/src/main/java/org/dimitri/task/TaskServiceApplication.java`

```java
package org.dimitri.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Point d'entrée du microservice Task.
 * 
 * @SpringBootApplication : Lance le contexte Spring, scan les composants.
 * @EnableDiscoveryClient : Enregistre ce service auprès de Eureka.
 */
@SpringBootApplication(scanBasePackages = "org.dimitri.task")
@EnableDiscoveryClient
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
```

---

## Étape 8 — Configuration (application.yml)

**Fichier** : `services/task-service/task-api/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: task-service

  # Configuration Eureka (découverte de services)
  cloud:
    discovery:
      client:
        simpleDiscoveryClient:
          order: 0
  
server:
  port: 8083  # Écouter sur le port 8083

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/  # URL du serveur Eureka
  instance:
    instance-id: task-service:${random.value}
```

---

## Étape 9 — Tests unitaires (mocking)

**Fichier** : `services/task-service/task-application/src/test/java/org/dimitri/task/application/service/TaskAppServiceTest.java`

```java
package org.dimitri.task.application.service;

import org.dimitri.task.application.exception.TaskNotFoundException;
import org.dimitri.task.application.ports.TaskWritePort;
import org.dimitri.task.domain.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du TaskAppService.
 * 
 * Utilisation de Mockito pour mocker le port TaskWritePort.
 * Bénéfices :
 * - Tests rapides (pas de DB)
 * - Tests indépendants (pas de dépendances externes)
 * - Tests focalisés sur la logique métier
 */
class TaskAppServiceTest {

    private TaskAppService taskAppService;
    private TaskWritePort mockTaskWritePort;

    @BeforeEach
    void setUp() {
        // Créer un mock du port
        mockTaskWritePort = Mockito.mock(TaskWritePort.class);

        // Injecter le mock au service
        taskAppService = new TaskAppService(mockTaskWritePort);
    }

    /**
     * Test : créer une tâche avec titre valide.
     */
    @Test
    void createTask_withValidInput_success() {
        // Arrange
        String title = "Buy groceries";
        String description = "Milk, eggs, bread";

        // Act
        Task result = taskAppService.createTask(title, description);

        // Assert
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(title, result.title());
        assertEquals(description, result.description());
        assertFalse(result.done());  // Initialement non-complétée

        // Vérifier que le port a été appelé une fois
        verify(mockTaskWritePort, times(1)).save(any(Task.class));
    }

    /**
     * Test : créer une tâche avec titre vide.
     */
    @Test
    void createTask_withBlankTitle_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            taskAppService.createTask("   ", "description");
        });

        // Vérifier que le port n'a PAS été appelé
        verify(mockTaskWritePort, never()).save(any(Task.class));
    }

    /**
     * Test : créer une tâche avec titre trop long.
     */
    @Test
    void createTask_withTooLongTitle_throwsException() {
        // Arrange
        String tooLongTitle = "a".repeat(256);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            taskAppService.createTask(tooLongTitle, "description");
        });

        verify(mockTaskWritePort, never()).save(any(Task.class));
    }

    /**
     * Test : marquer une tâche comme complétée.
     */
    @Test
    void completeTask_success() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Buy groceries", "Milk, eggs", false, null);
        Mockito.when(mockTaskWritePort.findById(taskId)).thenReturn(Optional.of(task));

        // Act
        Task result = taskAppService.completeTask(taskId);

        // Assert
        assertTrue(result.done());  // La tâche est maintenant complétée

        // Vérifier que le port update a été appelé
        verify(mockTaskWritePort, times(1)).update(any(Task.class));
    }

    /**
     * Test : marquer une tâche inexistante.
     */
    @Test
    void completeTask_taskNotFound_throwsException() {
        // Arrange
        UUID taskId = UUID.randomUUID();
        Mockito.when(mockTaskWritePort.findById(taskId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TaskNotFoundException.class, () -> {
            taskAppService.completeTask(taskId);
        });
    }
}
```

---

## Étape 10 — Tests d'intégration (Testcontainers + Postgres)

**Fichier** : `services/task-service/task-api/src/test/java/org/dimitri/task/controller/TaskControllerIntegrationTest.java`

```java
package org.dimitri.task.controller;

import org.dimitri.task.TaskServiceApplication;
import org.dimitri.task.domain.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.testcontainers.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration complets.
 * 
 * Utilise Testcontainers pour lancer une vraie BD Postgres.
 * Tests l'ensemble du flux : HTTP → Controller → Service → Repository → BD
 */
@SpringBootTest(
    classes = TaskServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
class TaskControllerIntegrationTest {

    /**
     * Container Postgres lancé automatiquement par Testcontainers.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Test : créer une tâche via POST /tasks.
     */
    @Test
    void createTask_success() {
        // Arrange
        TaskController.CreateTaskRequest req = new TaskController.CreateTaskRequest(
            "Buy groceries",
            "Milk, eggs, bread"
        );

        // Act
        ResponseEntity<TaskController.TaskDto> response = restTemplate.postForEntity(
            "/tasks",
            req,
            TaskController.TaskDto.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Buy groceries", response.getBody().getTitle());
        assertEquals("Milk, eggs, bread", response.getBody().getDescription());
        assertFalse(response.getBody().isDone());
    }

    /**
     * Test : récupérer toutes les tâches via GET /tasks.
     */
    @Test
    void getAllTasks_success() {
        // Arrange : créer deux tâches
        TaskController.CreateTaskRequest req1 = new TaskController.CreateTaskRequest("Task 1", "Desc 1");
        TaskController.CreateTaskRequest req2 = new TaskController.CreateTaskRequest("Task 2", "Desc 2");

        restTemplate.postForEntity("/tasks", req1, TaskController.TaskDto.class);
        restTemplate.postForEntity("/tasks", req2, TaskController.TaskDto.class);

        // Act
        ResponseEntity<TaskController.TaskDto[]> response = restTemplate.getForEntity(
            "/tasks",
            TaskController.TaskDto[].class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length >= 2);
    }

    /**
     * Test : marquer une tâche comme complétée.
     */
    @Test
    void completeTask_success() {
        // Arrange : créer une tâche
        TaskController.CreateTaskRequest req = new TaskController.CreateTaskRequest("Buy milk", "2 liters");
        ResponseEntity<TaskController.TaskDto> createResp = restTemplate.postForEntity(
            "/tasks",
            req,
            TaskController.TaskDto.class
        );
        UUID taskId = createResp.getBody().getId();

        // Act : marquer comme complétée
        ResponseEntity<TaskController.TaskDto> completeResp = restTemplate.exchange(
            "/tasks/" + taskId + "/complete",
            org.springframework.http.HttpMethod.PUT,
            null,
            TaskController.TaskDto.class
        );

        // Assert
        assertEquals(HttpStatus.OK, completeResp.getStatusCode());
        assertTrue(completeResp.getBody().isDone());
    }

    /**
     * Test : récupérer une tâche qui n'existe pas.
     */
    @Test
    void getTask_notFound() {
        // Act
        ResponseEntity<?> response = restTemplate.getForEntity(
            "/tasks/" + UUID.randomUUID(),
            Object.class
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

**Exécuter les tests** :
```bash
cd services/task-service
mvn test
```

---

## Étape 11 — Tester manuellement avec curl

### 1. Démarrer le service

```bash
cd services/task-service
mvn spring-boot:run
```

Vous devriez voir : `Started TaskServiceApplication in X seconds`

### 2. Créer une tâche

```bash
curl -X POST http://localhost:8083/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Buy groceries", "description": "Milk, eggs, bread"}'
```

Réponse attendue :
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Buy groceries",
  "description": "Milk, eggs, bread",
  "done": false,
  "createdAt": "2026-03-08T15:30:00Z"
}
```

### 3. Récupérer toutes les tâches

```bash
curl http://localhost:8083/tasks
```

Réponse attendue :
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "Buy groceries",
    "description": "Milk, eggs, bread",
    "done": false,
    "createdAt": "2026-03-08T15:30:00Z"
  }
]
```

### 4. Marquer comme complétée

```bash
curl -X PUT http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000/complete \
  -H "Content-Type: application/json"
```

Réponse attendue :
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Buy groceries",
  "description": "Milk, eggs, bread",
  "done": true,
  "createdAt": "2026-03-08T15:30:00Z"
}
```

### 5. Supprimer une tâche

```bash
curl -X DELETE http://localhost:8083/tasks/550e8400-e29b-41d4-a716-446655440000
```

Réponse attendue : HTTP 204 (No Content)

---

## Résumé & Checklist

### Fichiers créés

- ✅ `Task.java` (record immuable)
- ✅ `TaskWritePort.java` (interface)
- ✅ `TaskAlreadyExistsException.java` et `TaskNotFoundException.java`
- ✅ `InMemoryTaskRepository.java` (adapter en mémoire)
- ✅ `TaskAppService.java` (orchestration)
- ✅ `TaskController.java` (REST API)
- ✅ `TaskUseCaseConfig.java` (wiring Spring)
- ✅ `TaskServiceApplication.java` (bootstrap)
- ✅ `application.yml` (configuration)
- ✅ `TaskAppServiceTest.java` (unit tests)
- ✅ `TaskControllerIntegrationTest.java` (integration tests)

### Concepts appliqués

- ✅ Records immuables (Java 16+)
- ✅ Architecture hexagonale (ports & adapters)
- ✅ Injection de dépendances Spring
- ✅ REST API conventions
- ✅ DTOs pour sérialisation HTTP
- ✅ Unit tests avec Mockito
- ✅ Integration tests avec Testcontainers

### Next steps

1. **Lab 2** : Ajouter JWT au controller.
2. **Lab 3** : Remplacer InMemory par JPA + Postgres.
3. **Advanced** : Ajouter un port Outbox, publier des événements à Kafka.

Fin du Lab 1.
