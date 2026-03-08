# FAQ & Troubleshooting — Task Management Platform

Version 2026-03-08

Réponses aux questions fréquentes et solutions aux problèmes courants.

---

## Section 1 — Questions fondamentales sur l'architecture

### Q1.1 Pourquoi utiliser un record au lieu d'une classe ordinaire ?

**Réponse** :
Les records (Java 16+) offrent :
- ✅ Immuabilité garantie (plus sûr, pas de mutations accidentelles)
- ✅ Moins de boilerplate (pas besoin d'écrire getters/setters/equals/hashCode)
- ✅ Performance (compilateur optimise)
- ✅ Thread-safety (données immuables sont naturellement thread-safe)

**Exemple** :
```java
// Ancienne façon (verbeux)
public class Task {
    private UUID id;
    private String title;
    private boolean done;

    public Task(UUID id, String title, boolean done) { /* ... */ }
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public boolean isDone() { return done; }
    @Override public boolean equals(Object o) { /* ... */ }
    @Override public int hashCode() { /* ... */ }
    @Override public String toString() { /* ... */ }
}

// Nouvelle façon (concis)
public record Task(UUID id, String title, boolean done) { }
```

---

### Q1.2 Qu'est-ce qu'une "port" dans l'architecture hexagonale ?

**Réponse** :
Un **port** est une **interface** qui définit comment le domaine communique avec l'extérieur (BD, messagerie, API, etc.).

**Avantages** :
- Le domaine NE dépend PAS des détails d'implémentation (JDBC, JPA, etc.)
- On peut changer l'implémentation sans modifier le code métier
- On peut tester avec des mocks facilement

**Exemple** :
```java
// Port = interface pure (domaine ne sait pas comment c'est implémenté)
public interface TaskWritePort {
    void save(Task task);
    Optional<Task> findById(UUID id);
}

// Adapter 1 : implémentation en mémoire
@Repository
public class InMemoryTaskRepository implements TaskWritePort { /* ... */ }

// Adapter 2 : implémentation JDBC
@Repository
public class JdbcTaskRepository implements TaskWritePort { /* ... */ }

// Le domaine utilise le port ; Spring injecte l'adapter approprié
@Service
public class TaskAppService {
    private final TaskWritePort port;  // ← Ne sait pas si c'est mémoire ou JDBC
    // ...
}
```

---

### Q1.3 C'est quoi le pattern Outbox exactement ?

**Réponse** :
Le pattern Outbox **garantit la cohérence** quand un service doit modifier la BD ET publier un événement à Kafka.

**Problème sans Outbox** :
```
1. Sauvegarder User en BD ✅
2. Publier événement à Kafka... ❌ (crash)
→ Résultat : User créé, mais événement jamais envoyé
```

**Solution avec Outbox** :
```
1. (Transaction unique)
   a. Sauvegarder User en BD ✅
   b. Ajouter une ligne à la table outbox ✅
   (si erreur : tout roule back)

2. (Background job, async)
   - OutboxScheduler lit outbox chaque 5 secondes
   - Publie chaque ligne à Kafka
   - Marque comme processed=true
   - En cas d'erreur Kafka : retry

→ Résultat : User créé ET événement garantidement envoyé
```

**Diagramme de persistance** :
```
Table users:          Table outbox:
┌─────────────────┐   ┌────────────────────────────────┐
│ id: UUID        │   │ id: UUID                       │
│ username: str   │   │ aggregate_type: "User"         │
│ email: str      │   │ payload: JSON (event)          │
│ created_at: ts  │   │ processed: bool (false → true) │
└─────────────────┘   │ created_at: ts                 │
                      └────────────────────────────────┘

Quand on crée User:
→ INSERT INTO users (...) 
→ INSERT INTO outbox (...) [processed=false]
→ COMMIT (atomique)

OutboxScheduler toutes les 5 sec:
→ SELECT * FROM outbox WHERE processed=false
→ Pour chaque ligne :
   - publish à Kafka
   - UPDATE outbox SET processed=true WHERE id=...
```

---

### Q1.4 Pourquoi ConcurrentHashMap au lieu d'une Map ordinaire ?

**Réponse** :
`ConcurrentHashMap` est **thread-safe** (plusieurs threads peuvent y accéder simultanément).

**Exemple du problème** :
```java
// ❌ PAS thread-safe (race conditions possibles)
Map<UUID, Task> tasks = new HashMap<>();

// ✅ Thread-safe (Spring gère les requêtes concurrentes)
Map<UUID, Task> tasks = new ConcurrentHashMap<>();
```

Spring gère les requêtes HTTP concurrentes (chaque requête = thread). Plusieurs threads pourraient appeler `save()` simultanément → besoin de thread-safety.

---

## Section 2 — Questions sur JWT

### Q2.1 Comment fonctionne un JWT exactement ?

**Réponse** :
JWT = **JSON Web Token**. Structure : `Header.Payload.Signature`

**Exemple JWT** :
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJhbGljZSIsInJvbGVzIjpbIlVTRVIiXSwiaWF0IjoxNjc4ODkwNTM4LCJleHAiOjE2Nzg4OTQxMzh9.
gCvQ5D0kK5zF8nJ3pL9mX4rT6sU7vW2yZ1aB3cD5eF7
```

**Décodé** :
```
Header:   {"alg":"HS256","typ":"JWT"}
Payload:  {"sub":"alice","roles":["USER"],"iat":1678890538,"exp":1678894138}
Signature: HMAC-SHA256(base64(header).base64(payload), secret_key)
```

**Flux** :
```
1. Client appelle POST /auth/login {"username":"alice","password":"secret"}
2. Serveur valide credentials
3. Serveur crée JWT signé avec une clé secrète
4. Retourne token au client

5. Client ajoute JWT au header: Authorization: Bearer <token>
6. Pour chaque requête suivante, serveur :
   a. Extrait le JWT du header
   b. Valide la signature avec la clé secrète
   c. Vérifie que la date d'expiration n'est pas dépassée
   d. Extrait le subject (username) du payload
   e. Autorise la requête

Si signature invalide ou token expiré → 401 Unauthorized
```

---

### Q2.2 Où est stockée la clé secrète JWT ? Comment la sécuriser ?

**Réponse** :
En production, JAMAIS en hardcoded dans le code.

**Solutions** :
```yaml
# ❌ À NE PAS faire
SECRET_KEY: "super-secret-hardcoded"

# ✅ Bonnes pratiques
# Option 1 : Variable d'environnement
export JWT_SECRET=$(openssl rand -base64 32)

# Option 2 : fichier .env (ignore par git)
JWT_SECRET=...

# Option 3 : Azure Key Vault / AWS Secrets Manager
spring:
  cloud:
    azure:
      keyvault:
        secret:
          property-name: jwt.secret
```

**Implémentation sécurisée** :
```java
@Service
public class JwtService {
    @Value("${jwt.secret}")  // ← Lue depuis env ou application.yml (géré par secrets)
    private String secretKey;

    public String generateToken(String username) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        // ...
    }
}
```

---

## Section 3 — Problèmes courants et solutions

### P3.1 "Port 8083 déjà utilisé"

**Problème** :
```
Address already in use: bind
```

**Solutions** :
```powershell
# Option 1 : Trouver quel processus utilise le port
Get-NetTCPConnection -LocalPort 8083

# Option 2 : Tuer le processus
Stop-Process -Id <PID> -Force

# Option 3 : Changer le port dans application.yml
server:
  port: 8084  # Utiliser un autre port

# Option 4 : Redémarrer l'ordonnateur
Restart-Computer
```

---

### P3.2 "No qualifying bean of type TaskWritePort"

**Problème** :
```
org.springframework.beans.factory.NoSuchBeanDefinitionException: 
No qualifying bean of type 'org.dimitri.task.application.port.TaskWritePort'
```

**Cause** :
Spring ne trouve pas l'implémentation du port.

**Solutions** :
```java
// ✅ Assurer que InMemoryTaskRepository a @Repository
@Repository
public class InMemoryTaskRepository implements TaskWritePort { }

// ✅ Assurer que TaskServiceApplication scanne le bon package
@SpringBootApplication(scanBasePackages = "org.dimitri.task")

// ✅ Ou mettre @ComponentScan explicitement
@Configuration
@ComponentScan("org.dimitri.task")
public class AppConfig { }
```

---

### P3.3 "java.util.UUID cannot be deserialized from Object value"

**Problème** :
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000"  // ← String, pas UUID
}
```

Jackson ne sait pas convertir le String en UUID.

**Solution** :
```java
// Dans TaskDto ou CreateTaskRequest, ajouter un deserializer custom
public class TaskDto {
    @JsonDeserialize(using = UUIDDeserializer.class)
    private UUID id;
    // ...
}

// Ou utiliser ObjectMapper avec un registre
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());
mapper.findAndRegisterModules();
```

---

### P3.4 "Tâche créée mais POST /tasks retourne 400"

**Problème** :
```
HTTP 400 Bad Request
```

**Debugger** :
```java
// Dans le controller, ajouter des logs
@PostMapping
public ResponseEntity<TaskDto> createTask(@RequestBody CreateTaskRequest req) {
    System.out.println("Request received: " + req);
    System.out.println("Title: " + req.getTitle());
    System.out.println("Description: " + req.getDescription());
    
    if (req.getTitle() == null || req.getTitle().isBlank()) {
        return ResponseEntity.badRequest().build();
    }
    // ...
}
```

**Cause probable** :
- Title est vide ou null
- Content-Type n'est pas `application/json`
- Format JSON invalide

**Vérification curl** :
```powershell
# ✅ Correct
curl -X POST http://localhost:8083/tasks `
  -H "Content-Type: application/json" `
  -d '{"title": "My Task", "description": "..."}'

# ❌ Incorrect
curl -X POST http://localhost:8083/tasks `
  -d '{"title": "My Task"}'  # Content-Type manquant !
```

---

### P3.5 "Tests échouent avec Testcontainers"

**Problème** :
```
Testcontainers failed to start PostgreSQLContainer
```

**Vérifications** :
```bash
# 1. Docker doit être installé et actif
docker --version
docker ps

# 2. Windows : utiliser WSL 2 backend (pas Hyper-V legacy)
wsl --list --verbose

# 3. Augmenter le timeout dans le test
@Testcontainers
class MyTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withStartupTimeout(Duration.ofSeconds(60));  // ← Plus de temps
}

# 4. Vérifier qu'il y a assez d'espace disque
docker system df
```

---

### P3.6 "CompileError: target release 21 not supported"

**Problème** :
JDK installé n'est pas Java 21.

**Solution** :
```bash
# Vérifier la version JDK
java -version

# Télécharger Java 21 (LTS)
# https://www.oracle.com/java/technologies/downloads/#java21

# Ou utiliser SDKMAN
sdk install java 21.0.1-oracle
sdk use java 21.0.1-oracle

# Configurer Maven
export JAVA_HOME=C:\path\to\jdk21
```

---

## Section 4 — Performance & Optimisations

### Q4.1 Comment optimiser les requêtes SELECT ?

**Réponse** :
```java
// ❌ N+1 problem (lent)
List<Task> tasks = taskWritePort.findAll();
for (Task task : tasks) {
    System.out.println(task.id());  // 1 query pour findAll() + N queries
}

// ✅ Utiliser des indexes
CREATE INDEX idx_tasks_done ON tasks(done);

// ✅ Pagination pour de grands datasets
public List<Task> findAllPaginated(int page, int size) {
    int offset = page * size;
    // SELECT * FROM tasks LIMIT ? OFFSET ?
}

// ✅ Lazy loading avec JPA (à venir en Lab 3)
@Entity
public class Task {
    @ManyToOne(fetch = FetchType.LAZY)  // Charge seulement quand accédé
    private User owner;
}
```

---

### Q4.2 Combien de threads Spring Boot utilise par défaut ?

**Réponse** :
```yaml
# Par défaut : 200 threads (Tomcat)
server:
  tomcat:
    threads:
      max: 200    # Requests concurrentes max
      min-spare: 10

# Optimiser selon la charge
# API légère : 50-100
# API CPU-heavy : 10-20
# API I/O-heavy (DB, API calls) : 200+
```

---

## Section 5 — Sécurité

### Q5.1 Comment éviter les injections SQL en JDBC ?

**Réponse** :
```java
// ❌ Vulnérable (SQL injection)
String query = "SELECT * FROM users WHERE username = '" + username + "'";
jdbcTemplate.queryForObject(query, ...);

// ✅ Sûr (paramètres liés)
String query = "SELECT * FROM users WHERE username = ?";
jdbcTemplate.queryForObject(query, ..., username);
```

JdbcTemplate utilise des `PreparedStatement` qui échappent automatiquement.

---

### Q5.2 Comment logger les requêtes SQL pour debugger ?

**Réponse** :
```yaml
# application.yml
logging:
  level:
    org.springframework.jdbc.core: DEBUG
    # Ou pour Hibernate JPA (futur)
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

Ou via code :
```java
@Bean
public DataSourceLoggerInterceptor logInterceptor() {
    return new DataSourceLoggerInterceptor();
}
```

---

## Section 6 — Déploiement

### Q6.1 Comment créer une image Docker pour le service ?

**Réponse** :
```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copier le JAR compilé
COPY target/task-service-1.0.0.jar app.jar

# Exposer le port
EXPOSE 8083

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Build et run** :
```bash
# Build
mvn clean package

# Docker build
docker build -t task-service:1.0 .

# Docker run
docker run -p 8083:8083 task-service:1.0
```

---

### Q6.2 Comment déployer sur Azure Container Apps ?

**Réponse** :
```bash
# 1. Créer une Azure Container Registry
az acr create --resource-group myRG --name myRegistry --sku Basic

# 2. Build et push l'image
az acr build --registry myRegistry --image task-service:1.0 .

# 3. Créer une Container App
az containerapp create \
  --name task-service \
  --resource-group myRG \
  --image myRegistry.azurecr.io/task-service:1.0 \
  --target-port 8083
```

---

## Section 7 — Git & Versionning

### Q7.1 Comment versionner le projet ?

**Réponse** :
```bash
# Stratégie Semantic Versioning (Major.Minor.Patch)
# 1.0.0 = première release
# 1.0.1 = bugfix
# 1.1.0 = nouvelle feature
# 2.0.0 = breaking changes

# Tagging dans Git
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# Vérifier les tags
git tag -l
```

---

### Q7.2 Comment gérer les branches avec Git Flow ?

**Réponse** :
```bash
# Structure
main          ← Production (stable)
├── develop   ← Intégration (next release)
├── feature/* ← Features en cours
├── bugfix/*  ← Bugfixes
└── hotfix/*  ← Hotfixes critiques

# Workflow
git checkout develop
git checkout -b feature/add-jwt
# ... work ...
git commit -m "Add JWT to TaskController"
git push origin feature/add-jwt
# → Create Pull Request on GitHub
# → Review & Merge to develop

git checkout develop
git pull
# → Quand prêt pour release
git checkout -b release/1.1.0
# ... tests & final fixes ...
git checkout main
git merge release/1.1.0
git tag v1.1.0
git push origin main --tags
```

---

## Résumé — Checklist de résolution

Quand vous rencontrez un problème :

- [ ] Lire le message d'erreur complètement
- [ ] Rechercher sur Google ou Stack Overflow
- [ ] Vérifier les logs (`mvn clean install -X` pour debug)
- [ ] Isoler le problème (unit test ou minimal example)
- [ ] Demander dans une communauté (StackOverflow, Reddit, Discord)

Fin du FAQ & Troubleshooting.
