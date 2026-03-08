Lab 1 — Implement POST /tasks (student instructions)

Goal: implement a POST /tasks endpoint in `task-api`, store tasks in memory for the lab, and write unit tests.

Estimated time: 60–90 minutes

Prerequisites: the project runs (see `lecture_setup.md`).

Step 1 — Open project in IDE
- Open the project in IntelliJ IDEA or VS Code. Make sure `services/task-service` is visible as a module.

Step 2 — Add endpoint skeleton
- File: `services/task-service/task-api/src/main/java/org/dimitri/task/controller/TaskController.java`
- If class exists, add the POST method; if not, create it using sample below.

POST method to add (student should type this):

```java
@PostMapping
public ResponseEntity<Task> create(@RequestBody CreateTaskRequest req) {
    Task t = new Task(UUID.randomUUID(), req.getTitle(), false);
    // For the lab, use an in-memory map (Map<UUID, Task>) in a simple repo implementation
    inMemoryRepo.save(t);
    return ResponseEntity.status(HttpStatus.CREATED).body(t);
}
```

Step 3 — Implement in-memory repository
- Create a class `InMemoryTaskRepository` in `task-infrastructure` or a test package.
- Example:

```java
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

Step 4 — Wire the repository to the controller via constructor injection
- Use Spring `@Bean` in a `@Configuration` class to expose `InMemoryTaskRepository` as a `TaskRepository`.

Step 5 — Write unit tests
- Add JUnit 5 + Mockito tests for `TaskAppService` and controller (MockMvc) to verify POST behavior.

Sample unit test (MockMvc):

```java
@Test
void createTaskEndpoint() throws Exception {
    String json = "{\"title\":\"Do homework\"}";
    mockMvc.perform(post("/tasks").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Do homework"));
}
```

Step 6 — Run the service and test manually
- Start `task-api` and send a curl request:

```powershell
curl -X POST http://localhost:8082/tasks -H "Content-Type: application/json" -d '{"title":"Write lab report"}'
```

You should receive a 201 response with the created task JSON.

Step 7 — Commit and create Pull Request
- Add clear commit message: "lab1: add POST /tasks in-memory implementation and tests"

Grading criteria (for instructor):
- Endpoint exists and returns 201 (40%)
- Tests exist and pass (40%)
- Code is readable and documented (20%)

If students finish early: extend the lab to persist tasks to Postgres (use JPA) and add an integration test with Testcontainers.
