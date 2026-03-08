Lecture: Architecture deep-dive

This lecture goes into each system component, responsibilities, communication patterns, and trade-offs.

1) High-level components
- API Gateway (Spring Cloud Gateway): single entry point, routing, filters, simple auth enforcement
- Discovery Service (Eureka): dynamic service registration and discovery
- Config Service (Spring Cloud Config): central storage for application configuration (YAML)
- User Service & Task Service: domain services following hexagonal architecture
- Shared library: common DTOs, exceptions, utilities
- Frontend: React SPA calling the gateway
- Kafka: event backbone for asynchronous messaging
- Database (Postgres): primary persistence

2) Communication patterns
- Synchronous: HTTP between front → gateway → services
- Asynchronous: services publish domain events to Kafka (via outbox)

3) Hexagonal (Ports & Adapters)
- Domain: core business logic, pure Java classes/interfaces
- Ports: interfaces that express what the domain needs (e.g., TaskRepository)
- Adapters: concrete implementations (JdbcTaskRepository, JpaTaskRepository)

Benefits: testability, separation of concerns, easier to swap adapters.

4) Outbox pattern detailed
- Problem: ensure atomicity between DB write and broker publish
- Implementation steps:
  1. Within same DB transaction, write the business change and an outbox row with the serialized event payload
  2. A separate process (outbox-poller) reads unprocessed outbox rows, publishes to Kafka, and marks rows processed

5) Security
- Authentication: JWT (stateless), or OAuth2/OpenID Connect for production
- Authorization: roles-based checks at the controller or method level
- Secure actuator endpoints and configuration endpoints; never expose sensitive endpoints in production

6) Observability
- Health checks: Spring Boot Actuator
- Metrics: Prometheus exporter endpoint
- Tracing: OpenTelemetry + Jaeger for distributed trace spans

7) Data modelling
- Task entity: id (UUID), title, done flag, timestamps
- User entity: id, username, email, hashed password

8) Deployment considerations
- Docker images per service, use environment variables for configuration
- For production: externalize secrets (Vault), restrict actuator endpoints, enable HTTPS, configure readiness/liveness probes

9) Trade-offs and discussion topics
- Consistency vs availability when using async events
- When to use a shared library vs separate npm packages
- Pros/cons of service-per-module vs monorepo

Exercises
- Diagram the lifecycle of a "create task" request, including outbox writing and event publication
- Identify possible failure points and propose retry/backoff strategies
