Syllabus — Task Management Platform course

Audience: beginners in software engineering, 3–6 hour workshop format.

Session 0 — Pre-class setup (homework)
- Install JDK 21, Maven, Node.js, Docker
- Clone the repository
- Verify basic commands (`java --version`, `mvn --version`, `docker --version`)

Session 1 (60–90 min) — Intro & Architecture
- Objectives: understand repo structure, multi-module Maven, hexagonal architecture
- Topics: POM parent/BOM, Spring Boot, Eureka, Gateway, Config Service, Outbox pattern
- Activity: read `lecture_architecture.md`, discuss diagram

Session 2 (90–120 min) — Hands-on setup & run
- Objectives: run infrastructure (DB, Kafka), start services, run frontend
- Tasks: follow `lecture_setup.md` to run docker compose, build backend, start services
- Activity: smoke tests — curl GET /tasks, open frontend

Session 3 (120–180 min) — Lab: implement and test API
- Objectives: implement POST /tasks, write unit tests and integration test
- Tasks: follow `lab1_create_api.md`; commit and create PR
- Assessment: pass unit tests and endpoint functions

Session 4 (60 min) — Security & CI
- Objectives: add JWT protection to endpoints, add CI pipeline to build & test
- Tasks: follow `security.md` and `ci_cd.md`

Optional advanced sessions
- Persistence (JPA + Flyway migration)
- Testcontainers for integration tests
- Tracing/observability (OpenTelemetry, Jaeger)

Evaluation: practical deliverables (endpoint + tests), code quality, documentation

Materials provided: slides (instructor), course-pack markdown files, solution examples
