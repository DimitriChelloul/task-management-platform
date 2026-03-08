Lecture: Environment setup — Step-by-step

Goal: Make sure every student has the same working environment and can run the project locally.

1) Clone the repository

```powershell
cd %USERPROFILE%\IdeaProjects
git clone <repo-url> task-management-platform
cd task-management-platform
```

2) Install Java (JDK 21)
- Windows: download from Temurin (https://adoptium.net) or use winget/chocolatey
- Verify:
```powershell
java --version
```

3) Install Maven
- https://maven.apache.org/download.cgi
- Verify:
```powershell
mvn --version
```

4) Install Node.js (for frontend)
- https://nodejs.org/
- Verify:
```powershell
node --version
npm --version
```

5) Install Docker Desktop
- https://www.docker.com/get-started
- Verify:
```powershell
docker --version
docker compose version
```

6) (Optional) Pandoc + MiKTeX for PDF generation
- Pandoc: https://pandoc.org/installing.html
- MiKTeX: https://miktex.org/download
- Verify:
```powershell
pandoc --version
where.exe xelatex
```

7) Start infrastructure (instructor will ensure docker images are accessible)

```powershell
# Postgres
docker compose -f docker-compose.db.yml up -d
# Kafka
docker compose -f docker-compose.kafka.yml up -d
```

8) Build backend once to avoid long compilation during the workshop

```powershell
mvn -T 1C clean install -DskipTests
```

9) Start discovery service, gateway, and example services (students should use separate terminals)

```powershell
cd infrastructure\discovery-service
mvn spring-boot:run
# new terminal
cd infrastructure\api-gateway
mvn spring-boot:run
# new terminal
cd services\user-service\user-api
mvn spring-boot:run
# new terminal
cd services\task-service\task-api
mvn spring-boot:run
```

10) Start frontend

```powershell
cd frontend
npm ci
npm start
```

11) Quick smoke tests
- Gateway: `http://localhost:8082/tasks`
- Frontend: `http://localhost:3000`

Troubleshooting tips
- Container logs: `docker compose -f docker-compose.db.yml logs --tail=200`
- JVM port conflicts: change port in `application.yml` under `server.port`
- Service not registering in Eureka: check `spring.application.name` and logs for connection issues
