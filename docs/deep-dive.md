# Deep-Dive complet — Task Management Platform

Version 2026-03-08

Ce document scanne et explique en détail CHAQUE partie du code existant, l'architecture hexagonale, le pattern Outbox, la sécurité JWT, Kafka, et les migrations. C'est un guide complet pour comprendre et reproduire l'application.

---

## Part 1 — Architecture globale expliquée en détail

### 1.1 Pourquoi cette architecture ?

Le projet utilise **l'architecture hexagonale** (ports & adapters) combinée à **la découverte de services** (Eureka) et **l'Outbox pattern** pour garantir la cohérence des données et des événements dans un système distribué.

**Problème résolu** :
- Services indépendants besoin de communiquer sans être fortement couplés.
- Quand un service modifie la BD ET doit envoyer un événement (Kafka), comment garantir que l'événement est envoyé si la BD est modifiée mais le message broker échoue ?

**Solution** : Outbox pattern + événements asynchrones.

### 1.2 Vue d'ensemble des composants

```
┌─────────────────────────────────────────────────────────────────┐
│                         Frontend (React)                         │
│                    (http://localhost:3000)                       │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway (Port 8082)                      │
│  - Route /users/** → USER-SERVICE (Eureka lookup)               │
│  - Route /tasks/** → TASK-SERVICE (Eureka lookup)               │
│  - JWT validation (JwtWebFilter)                                 │
└────────────┬──────────────────────────────┬────────────────────┘
             │                              │
    HTTP     ▼                    HTTP       ▼
┌──────────────────────┐      ┌──────────────────────┐
│   USER-SERVICE       │      │   TASK-SERVICE       │
│   (Port 8081)        │      │   (Port 8083)        │
│                      │      │                      │
│ - Create User        │      │ - Create Task        │
│ - List Users         │      │ - List Tasks         │
│ - Auth / Login       │      │ - Update Task        │
│ - Outbox Scheduler   │      │                      │
└──┬───────────────────┘      └──────┬───────────────┘
   │                                  │
   └──────────────┬───────────────────┘
                  │ (JDBC write)
                  ▼
         ┌─────────────────────┐
         │    Postgres DB      │
         │                     │
         │ - users table       │
         │ - outbox table      │
         │ - tasks table       │
         └────────┬────────────┘
                  │ (SQL init scripts)
                  │
            docker/init/*.sql
```

---

## Part 2 — Architecture hexagonale du user-service (exemple complet)

### 2.1 Structure des dossiers

```
user-service/
├── user-api/                 # Point d'entrée (controllers REST, wiring)
│   ├── pom.xml              # Spring Boot Starter Web, security, etc.
│   ├── src/main/java/...
│   │   ├── UserServiceApplication.java   # @SpringBootApplication
│   │   ├── config/
│   │   │   └── UserUseCaseConfig.java    # Spring @Bean wiring
│   │   ├── controller/
│   │   │   ├── UserController.java       # REST endpoints
│   │   │   ├── AuthController.java       # Login, JWT generation
│   │   │   └── KafkaTestController.java  # Dev/test
│   │   ├── api/
│   │   │   ├── security/
│   │   │   │   └── JwtService.java       # JWT generation/validation
│   │   │   ├── kafka/
│   │   │   │   └── UserEventProducer.java # Kafka publisher
│   │   │   ├── events/
│   │   │   │   └── UserCreatedEvent.java # DTO for events
│   │   │   └── KafkaTopicsProperties.java # Config for topics
│   │   ├── infrastructure/
│   │   │   ├── memory/
│   │   │   │   ├── InMemoryUserWriteAdapter.java   # Adapter pour user persistence
│   │   │   │   └── InMemoryOutboxWriteAdapter.java # Adapter pour outbox
│   │   │   ├── outbox/
│   │   │   │   └── OutboxScheduler.java  # Scheduler qui lit outbox → Kafka
│   │   │   └── jdbc/
│   │   │       ├── JdbcUserRepository.java      # JDBC impl
│   │   │       └── JdbcOutboxRepository.java    # JDBC impl
│   │   └── adapters/in/
│   │       └── UserCommandAdapter.java  # Adapter for ports
│   ├── resources/
│   │   └── application.yml      # Configuration (port 8081, Eureka, Kafka)
│   └── target/                  # Build output (ignore)
│
├── user-application/            # Logique applicative (use-cases)
│   ├── pom.xml
│   ├── src/main/java/...
│   │   ├── service/
│   │   │   ├── UserAppService.java          # Service applicatif principal
│   │   │   └── UserCommandPort.java         # Interface des commandes
│   │   ├── usecase/
│   │   │   └── CreateUserUseCase.java       # Use-case "créer utilisateur"
│   │   └── ports/
│   │       ├── UserWritePort.java           # Port for persistence
│   │       ├── OutboxWritePort.java         # Port for outbox write
│   │       └── OutboxReadPort.java          # Port for outbox read
│   └── target/
│
├── user-domain/                 # Logique métier (entities, règles)
│   ├── pom.xml
│   ├── src/main/java/...
│   │   ├── domain/
│   │   │   ├── User.java                    # Record/Entity
│   │   │   ├── OutboxEvent.java             # Event entity
│   │   │   ├── port/in/
│   │   │   │   └── UserCommandPort.java     # Port (interface)
│   │   │   └── exception/
│   │   │       └── UserAlreadyExistsException.java
│   └── target/
│
└── user-infrastructure/         # Implémentations techniques (JDBC, JPA)
    ├── pom.xml
    ├── src/main/java/...
    │   ├── persistence/
    │   │   ├── UserRepository.java          # Interface
    │   │   └── OutboxRepository.java        # Interface
    │   ├── jdbc/
    │   │   ├── JdbcUserRepository.java      # Impl JDBC
    │   │   └── JdbcOutboxRepository.java    # Impl JDBC
    │   └── migration/
    │       └── V1__init.sql                 # Flyway migration
    └── target/
```

### 2.2 Flux d'une requête POST /users (créer utilisateur)

**Diagramme de flux** :

```
1. Client appelle : POST /users {"username": "alice", "email": "alice@example.com"}
                    ↓
2. UserController.create() (user-api)
   ├─ Valide le JSON
   └─ Appelle UserAppService.createUser()
                    ↓
3. UserAppService (user-application)
   ├─ Valide la logique métier (ex: pas de doublon)
   ├─ Crée une entité User (user-domain)
   ├─ Appelle le port UserWritePort.save(user)
   └─ Appelle le port OutboxWritePort.save(event)
                    ↓
4. Adapters (user-api/infrastructure/memory/ ou jdbc/)
   ├─ InMemoryUserWriteAdapter OU JdbcUserRepository
   │  └─ Persiste user dans la Map (mémoire) ou BD (JDBC)
   │
   └─ InMemoryOutboxWriteAdapter OU JdbcOutboxRepository
      └─ Persiste l'événement dans la table outbox
                    ↓
5. Dans la même transaction DB (ou atomiquement en mémoire) :
   ├─ User sauvegardé
   └─ Event ajouté à la table outbox
                    ↓
6. OutboxScheduler (background job) exécute toutes les secondes :
   ├─ Lit les lignes non-traitées dans outbox
   ├─ Publie chaque événement dans Kafka (topic "user-events")
   ├─ Marque la ligne comme processed (processed=true)
   └─ En cas d'erreur, retry avec backoff exponentiel
                    ↓
7. UserEventProducer envoie à Kafka
   └─ Autres services peuvent écouter "user-events" et réagir
                    ↓
8. Réponse HTTP 201 Created avec l'entité User créée
```

---

## Part 3 — Code détaillé expliqué ligne par ligne

### 3.1 UserServiceApplication.java (Bootstrap)

```java
// Fichier : services/user-service/user-api/src/main/java/.../UserServiceApplication.java

@SpringBootApplication(scanBasePackages = "org.dimitri.user")
@EnableScheduling                         // ← Active les tâches planifiées (OutboxScheduler)
@EnableConfigurationProperties(KafkaTopicsProperties.class)  // ← Charge les topics Kafka depuis application.yml
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

**Explications** :
- `@SpringBootApplication` : Lance le contexte Spring. Les beans sont découverts dans le package `org.dimitri.user`.
- `@EnableScheduling` : Permet les méthodes `@Scheduled` (ex: OutboxScheduler qui tourne toutes les secondes).
- `@EnableConfigurationProperties(KafkaTopicsProperties.class)` : Injecte les propriétés Kafka depuis `application.yml`.

### 3.2 User.java (Domain entity)

```java
// Fichier : services/user-service/user-domain/.../domain/User.java

public record User(
    UUID id,
    String username,
    String email,
    String passwordHash,
    OffsetDateTime createdAt
) {
    // Record = classe immuable générée automatiquement avec equals(), hashCode(), toString()
    // Bénéfices : moins de boilerplate, sécurité par immuabilité

    public static User create(String username, String email, String passwordHash) {
        return new User(
            UUID.randomUUID(),
            username,
            email,
            passwordHash,
            OffsetDateTime.now()
        );
    }
}
```

**Explications** :
- `record` (Java 16+) : génère automatiquement constructor, getters, equals(), hashCode(), toString().
- `UUID` : identifiant unique, préféré aux auto-increment en microservices (pas de collision).
- `OffsetDateTime` : timestamp avec timezone, plus robuste que `java.util.Date`.

### 3.3 UserWritePort.java (Port — interface métier)

```java
// Fichier : services/user-service/user-application/.../ports/UserWritePort.java

public interface UserWritePort {
    /**
     * Persiste un nouvel utilisateur.
     * @param user l'entité User (avec ID généré)
     * @throws UserAlreadyExistsException si username existe déjà
     */
    void save(User user) throws UserAlreadyExistsException;

    /**
     * Récupère un utilisateur par son username.
     * @param username
     * @return Optional (absent si non trouvé)
     */
    Optional<User> findByUsername(String username);
}
```

**Explications** :
- Port = interface définie dans la couche `application` / `domain`.
- Abstraite : la couche métier NE connaît PAS si on use JDBC, JPA, ou une API REST.
- Bénéfices : testabilité (mocker le port), flexibilité (swap l'implémentation facilement).

### 3.4 JdbcUserRepository.java (Adapter — implémentation JDBC)

```java
// Fichier : services/user-service/user-api/.../infrastructure/jdbc/JdbcUserRepository.java

@Repository  // Spring component
public class JdbcUserRepository implements UserWritePort {
    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(User user) throws UserAlreadyExistsException {
        // Vérification d'unicité
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username = ?",
            Integer.class,
            user.username()
        );
        if (count != null && count > 0) {
            throw new UserAlreadyExistsException("User " + user.username() + " already exists");
        }

        // Insertion
        jdbcTemplate.update(
            "INSERT INTO users (id, username, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)",
            user.id().toString(),
            user.username(),
            user.email(),
            user.passwordHash(),
            Timestamp.from(user.createdAt().toInstant())
        );
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            User user = jdbcTemplate.queryForObject(
                "SELECT id, username, email, password_hash, created_at FROM users WHERE username = ?",
                (rs, rowNum) -> new User(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC)
                ),
                username
            );
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
```

**Explications** :
- `@Repository` : Spring stereotype, traduit les exceptions JDBC en DataAccessExceptions.
- `JdbcTemplate` : Spring wrapper autour de JDBC pour éviter boilerplate.
- `RowMapper` (lambda) : mappe les colonnes SQL vers l'objet User.
- Port satisfait (UserWritePort implémenté) : le domaine NE sait pas que c'est JDBC.

### 3.5 UserAppService.java (Service applicatif — orchestration)

```java
// Fichier : services/user-service/user-application/.../service/UserAppService.java

@Service  // Spring component
public class UserAppService {
    private final UserWritePort userWritePort;
    private final OutboxWritePort outboxWritePort;
    private final JwtService jwtService;

    public UserAppService(UserWritePort userWritePort, OutboxWritePort outboxWritePort, JwtService jwtService) {
        this.userWritePort = userWritePort;
        this.outboxWritePort = outboxWritePort;
        this.jwtService = jwtService;
    }

    /**
     * Use-case : créer un nouvel utilisateur et émettre un événement.
     */
    @Transactional  // ← CRUCIAL : assure l'atomicité (User + Outbox dans la même transaction)
    public CreateUserResponse createUser(String username, String email, String plainPassword) {
        // 1. Créer l'entité User
        String passwordHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User newUser = User.create(username, email, passwordHash);

        // 2. Persister dans la BD
        userWritePort.save(newUser);

        // 3. Créer un événement
        UserCreatedEvent event = new UserCreatedEvent(newUser.id(), username, email);

        // 4. Persister l'événement dans la table outbox (même transaction!)
        outboxWritePort.save(OutboxEvent.fromEvent(event));

        // 5. Générer un JWT (optionnel : login immédiatement après création)
        String token = jwtService.generateToken(username);

        return new CreateUserResponse(newUser.id(), username, email, token);
    }

    /**
     * Authentifier l'utilisateur et générer un JWT.
     */
    public String authenticate(String username, String password) {
        User user = userWritePort.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!BCrypt.checkpw(password, user.passwordHash())) {
            throw new BadCredentialsException("Invalid password");
        }

        return jwtService.generateToken(username);
    }
}
```

**Points clés** :
- `@Transactional` : Spring gère la transaction; si une exception → rollback de tout (User + Outbox).
- BCrypt : hash sécurisé du mot de passe (jamais stocker en plaintext).
- Outbox atomique : User et Event sauvegardés ou rien — garantit la cohérence.

### 3.6 OutboxEvent.java (Domain entity pour l'Outbox pattern)

```java
// Fichier : services/user-service/user-domain/.../domain/OutboxEvent.java

public record OutboxEvent(
    UUID id,
    String aggregateType,        // ex: "User"
    String payload,              // JSON sérialisé de l'événement
    boolean processed,           // false initialement, true après publication Kafka
    OffsetDateTime createdAt
) {
    public static OutboxEvent fromEvent(UserCreatedEvent event) {
        return new OutboxEvent(
            UUID.randomUUID(),
            "User",
            serializeEvent(event),  // ObjectMapper.writeValueAsString(event)
            false,
            OffsetDateTime.now()
        );
    }

    private static String serializeEvent(UserCreatedEvent event) {
        // Utiliser ObjectMapper pour convertir en JSON
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(event);
    }
}
```

**Pattern Outbox expliqué** :
- Problème: quand créer User ET publier l'événement dans Kafka?
  - Si on fait: save User → publish event : risque = crash entre save et publish → événement perdu.
- Solution: dans la même transaction, save User ET une ligne dans la table outbox.
- Background job (OutboxScheduler) lit la table outbox, publie à Kafka, marque processed=true.

### 3.7 OutboxScheduler.java (Background job)

```java
// Fichier : services/user-service/user-api/.../infrastructure/outbox/OutboxScheduler.java

@Component
public class OutboxScheduler {
    private final OutboxReadPort outboxReadPort;
    private final UserEventProducer userEventProducer;
    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    public OutboxScheduler(OutboxReadPort outboxReadPort, UserEventProducer userEventProducer) {
        this.outboxReadPort = outboxReadPort;
        this.userEventProducer = userEventProducer;
    }

    /**
     * Tous les 5 secondes, lire l'outbox et publier les événements à Kafka.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    @Transactional
    public void pollAndPublish() {
        log.debug("Polling outbox for unprocessed events...");

        List<OutboxEvent> unprocessed = outboxReadPort.findUnprocessed();
        log.debug("Found {} unprocessed events", unprocessed.size());

        for (OutboxEvent event : unprocessed) {
            try {
                // Publier à Kafka
                userEventProducer.publish(event);

                // Marquer comme traité
                outboxReadPort.markAsProcessed(event.id());
                log.info("Published and marked event {} as processed", event.id());
            } catch (Exception e) {
                log.error("Failed to publish event {}, will retry later", event.id(), e);
                // En production : implémenter un retry avec backoff exponentiel
                // ex: max_attempts, exponential_backoff_ms, etc.
            }
        }
    }
}
```

**Comment ça fonctionne** :
1. `@Scheduled(fixedDelay = 5000)` : exécute la méthode toutes les 5 secondes.
2. `findUnprocessed()` : SELECT * FROM outbox WHERE processed=false.
3. Pour chaque event : publier à Kafka.
4. Si succès : `markAsProcessed()` → UPDATE outbox SET processed=true.
5. Si erreur : log et retry à la prochaine exécution (simple mais fonctionne).

### 3.8 UserController.java (REST API)

```java
// Fichier : services/user-service/user-api/.../controller/UserController.java

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserAppService userAppService;

    public UserController(UserAppService userAppService) {
        this.userAppService = userAppService;
    }

    /**
     * POST /users — créer un nouvel utilisateur
     * @param req: {"username": "alice", "email": "alice@example.com", "password": "secret"}
     */
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest req) {
        CreateUserResponse res = userAppService.createUser(
            req.getUsername(),
            req.getEmail(),
            req.getPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * GET /users/{username} — récupérer un utilisateur
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUser(@PathVariable String username) {
        // Appeler un port ReadPort (non montré ici)
        UserDto dto = userAppService.getUserByUsername(username);
        return ResponseEntity.ok(dto);
    }

    public static class CreateUserRequest {
        private String username;
        private String email;
        private String password;
        // getters/setters
    }

    public static class CreateUserResponse {
        private UUID id;
        private String username;
        private String email;
        private String token;
        // ...
    }
}
```

---

## Part 4 — JWT Security détaillé

### 4.1 JwtService.java (Génération et validation de JWT)

```java
// Fichier : services/user-service/user-api/.../api/security/JwtService.java

@Service
public class JwtService {
    private static final String SECRET_KEY = "your-super-secret-key-should-be-in-env-variable"; // DANGER in prod!
    private static final long EXPIRATION_TIME = 3600000; // 1 heure en ms

    private final JwtParser jwtParser;
    private final JwtBuilder jwtBuilder;

    public JwtService() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        Key key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        this.jwtBuilder = Jwts.builder().signWith(key, SignatureAlgorithm.HS256);
    }

    /**
     * Générer un JWT pour un utilisateur.
     * @param username
     * @return token signé
     */
    public String generateToken(String username) {
        return jwtBuilder
            .setSubject(username)                          // ← claim principal
            .claim("roles", List.of("USER"))              // ← claim custom (rôles)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .compact();                                    // ← sérialiser + signer
    }

    /**
     * Valider et extraire le username du JWT.
     * @param token
     * @return username (subject du token)
     * @throws JwtException si invalide ou expiré
     */
    public String validateAndGetUsername(String token) {
        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();
            return claims.getSubject();
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid JWT: " + e.getMessage());
        }
    }

    public List<String> getRoles(String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        return (List<String>) claims.get("roles");
    }
}
```

**JWT Structure** :
```
Header.Payload.Signature

Header:        {"alg":"HS256","typ":"JWT"}
Payload:       {"sub":"alice","roles":["USER"],"iat":1234567890,"exp":1234571490}
Signature:     HMAC-SHA256(base64(header).base64(payload), secret)
```

### 4.2 JwtWebFilter.java (Gateway filter — valider JWT à chaque requête)

```java
// Fichier : infrastructure/api-gateway/.../security/JwtWebFilter.java

@Component
public class JwtWebFilter implements WebFilter {
    private final JwtService jwtService;

    public JwtWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 1. Extraire le JWT du header Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // 2. Valider le token
                String username = jwtService.validateAndGetUsername(token);
                List<String> roles = jwtService.getRoles(token);

                // 3. Créer un principal authentifié et l'ajouter au contexte
                Principal principal = new Principal() {
                    @Override
                    public String getName() {
                        return username;
                    }
                };
                exchange.getAttributes().put("principal", principal);
                exchange.getAttributes().put("roles", roles);
                // Continuer la chaîne de filtres
                return chain.filter(exchange);
            } catch (JwtException e) {
                // JWT invalide
                return handleError(exchange, "Invalid JWT", HttpStatus.UNAUTHORIZED);
            }
        } else {
            // Pas de JWT → accès public, ou rediriger vers /auth/login
            return chain.filter(exchange);
        }
    }

    private Mono<Void> handleError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(message.getBytes()))
        );
    }
}
```

---

## Part 5 — Kafka & Outbox pattern en profondeur

### 5.1 UserEventProducer.java (Publier événements à Kafka)

```java
// Fichier : services/user-service/user-api/.../api/kafka/UserEventProducer.java

@Service
public class UserEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties kafkaProps;
    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.java);

    public UserEventProducer(KafkaTemplate<String, String> kafkaTemplate, KafkaTopicsProperties kafkaProps) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProps = kafkaProps;
    }

    /**
     * Publier un événement d'outbox à Kafka.
     * @param event
     */
    public void publish(OutboxEvent event) {
        try {
            String topicName = kafkaProps.getUserEvents();  // "user-events" from application.yml
            String message = event.payload();

            // Envoyer le message de manière asynchrone
            kafkaTemplate.send(topicName, message)
                .addCallback(
                    result -> log.info("Message sent to Kafka topic: {}", topicName),
                    ex -> log.error("Failed to send message to Kafka", ex)
                );
        } catch (Exception e) {
            log.error("Error publishing event to Kafka", e);
            throw new RuntimeException("Kafka publish failed", e);
        }
    }
}
```

### 5.2 KafkaTopicsProperties.java (Configuration externalisée)

```java
// Fichier : services/user-service/user-api/.../api/KafkaTopicsProperties.java

@Configuration
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {
    private String userEvents = "user-events";      // Topic name
    private String taskEvents = "task-events";

    public String getUserEvents() {
        return userEvents;
    }

    public void setUserEvents(String userEvents) {
        this.userEvents = userEvents;
    }

    // ... getters/setters
}
```

**Dans `application.yml`** :
```yaml
kafka:
  bootstrap-servers: localhost:9092
  topics:
    user-events: user-events
    task-events: task-events
```

---

## Part 6 — Database schema & Flyway migrations

### 6.1 SQL Schema (V1__init.sql dans Flyway)

```sql
-- Fichier : services/user-service/user-infrastructure/src/main/resources/db/migration/V1__init.sql

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(200) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_username ON users(username);

CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,         -- "User", "Task", etc.
    payload TEXT NOT NULL,                        -- JSON serialized event
    processed BOOLEAN DEFAULT FALSE,              -- true after publishing to Kafka
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_processed ON outbox(processed, created_at);
-- Index sur (processed, created_at) pour trouver rapidement les non-traités
```

**Flyway workflow** :
1. Au démarrage du service, Flyway scanne `db/migration/`.
2. Exécute les migrations SQL non-exécutées (V1__, V2__, etc.).
3. Enregistre dans la table `flyway_schema_history` pour éviter les re-exécutions.
4. Si migration échoue → service refuse de démarrer (fail-fast).

---

## Part 7 — Testing (JUnit + Testcontainers)

### 7.1 Unit test simple (UserAppService)

```java
// Fichier : services/user-service/user-application/src/test/java/.../service/UserAppServiceTest.java

class UserAppServiceTest {
    private UserAppService userAppService;
    private UserWritePort mockUserPort;
    private OutboxWritePort mockOutboxPort;
    private JwtService mockJwtService;

    @BeforeEach
    void setUp() {
        mockUserPort = Mockito.mock(UserWritePort.class);
        mockOutboxPort = Mockito.mock(OutboxWritePort.class);
        mockJwtService = Mockito.mock(JwtService.class);
        userAppService = new UserAppService(mockUserPort, mockOutboxPort, mockJwtService);
    }

    @Test
    void createUser_success() throws Exception {
        // Arrange
        String username = "alice";
        String email = "alice@example.com";
        String password = "secure123";

        Mockito.when(mockUserPort.findByUsername(username))
            .thenReturn(Optional.empty());  // User doesn't exist yet

        Mockito.when(mockJwtService.generateToken(username))
            .thenReturn("fake-jwt-token");

        // Act
        CreateUserResponse response = userAppService.createUser(username, email, password);

        // Assert
        assertNotNull(response);
        assertEquals(username, response.getUsername());
        assertEquals("fake-jwt-token", response.getToken());

        // Vérifier que save a été appelé
        Mockito.verify(mockUserPort, Mockito.times(1)).save(Mockito.any(User.class));
        Mockito.verify(mockOutboxPort, Mockito.times(1)).save(Mockito.any(OutboxEvent.class));
    }

    @Test
    void createUser_duplicateUsername_throwsException() throws Exception {
        // Arrange
        String username = "alice";
        Mockito.when(mockUserPort.findByUsername(username))
            .thenReturn(Optional.of(Mockito.mock(User.class))); // User already exists

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            userAppService.createUser(username, "new@example.com", "password");
        });
    }
}
```

### 7.2 Integration test avec Testcontainers (Postgres réelle)

```java
// Fichier : services/user-service/user-api/src/test/java/.../integration/UserServiceIntegrationTest.java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserAppService userAppService;

    @Test
    void createUserEndToEnd() {
        // Arrange
        CreateUserRequest req = new CreateUserRequest("bob", "bob@example.com", "secret123");

        // Act
        ResponseEntity<CreateUserResponse> response = restTemplate.postForEntity(
            "/users",
            req,
            CreateUserResponse.class
        );

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("bob", response.getBody().getUsername());
    }

    @Test
    void userPersistsInDatabase() throws Exception {
        // Act
        userAppService.createUser("charlie", "charlie@example.com", "secret");

        // Assert (vérifier la BD)
        // Requête SQL pour confirmer que l'utilisateur est bien en BD
        JdbcTemplate jdbcTemplate = restTemplate.getRestTemplate()
            .getInterceptors().stream()
            .findFirst()
            .ifPresentOrElse(
                i -> { /* ... */ },
                () -> System.out.println("Check DB manually or use @Autowired JdbcTemplate")
            );
    }
}
```

**Testcontainers** :
- Lance une vraie BD Postgres dans un container Docker (automatiquement).
- Chaque test tourne en isolation (BD fraîche).
- Parfait pour tester les repositories et les interactions DB réelles.

---

## Part 8 — Exercices pratiques & Labs

### Lab 1 — Implémenter POST /tasks

**Objectif** : créer un endpoint POST /tasks similaire à POST /users.

**Étapes** :
1. Créer la classe `Task` (domain) — immuable avec UUID, title, done flag.
2. Créer l'interface `TaskWritePort` (application) — method save(Task).
3. Implémenter `InMemoryTaskRepository` ou `JdbcTaskRepository` (infrastructure).
4. Créer `TaskAppService` qui orchestre.
5. Créer `TaskController` avec `@PostMapping("/tasks")`.
6. Écrire des tests unitaires (mock repository) et des tests d'intégration (Testcontainers).
7. Tester manuellement via curl ou Postman.

**Commandes de test** :
```bash
curl -X POST http://localhost:8083/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Write documentation", "done": false}'
```

### Lab 2 — Ajouter JWT aux endpoints

**Objectif** : protéger POST /tasks et GET /tasks avec JWT.

**Étapes** :
1. Dans TaskController, ajouter un paramètre `@RequestHeader("Authorization") String token`.
2. Valider le token avec JwtService.
3. Extraire le username du JWT.
4. Logger ou tracker qui a créé la tâche (optionnel : ajouter `createdBy` à Task).
5. Écrire des tests : requête sans token → 401, avec JWT valide → 201.

**Commandes de test** :
```bash
# Étape 1 : se logger (POST /auth/login) pour obtenir un token
TOKEN=$(curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret"}' \
  | jq -r '.token')

# Étape 2 : utiliser le token pour créer une tâche
curl -X POST http://localhost:8083/tasks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title": "Important task"}'
```

### Lab 3 — Implémentation JPA & Flyway (avancé)

**Objectif** : remplacer l'implémentation en mémoire par JPA + DB.

**Étapes** :
1. Créer `@Entity Task` avec annotations JPA (@Id, @Column, etc.).
2. Créer `TaskJpaRepository extends JpaRepository<Task, UUID>`.
3. Créer une migration Flyway V1__task_schema.sql.
4. Modifier TaskAppService pour utiliser le repository JPA.
5. Lancer les tests d'intégration avec Testcontainers.

---

## Part 9 — Résumé et checklist finale

### Concepts clés résumés

| Concept | Pourquoi | Où |
|---------|---------|-----|
| **Hexagonal Architecture** | Découpler domaine de l'infra | *-api, *-application, *-domain, *-infrastructure |
| **Ports & Adapters** | Interfaces métier indépendantes | Ports = interfaces en application, Adapters = impls |
| **Outbox Pattern** | Garantir atomicité event + DB | OutboxEvent, OutboxScheduler, Kafka |
| **JWT** | Auth stateless | JwtService, JwtWebFilter |
| **Testcontainers** | Tests d'intégration réalistes | @Testcontainers, PostgreSQLContainer |
| **Flyway** | Versioning de schémas DB | db/migration/V*__.sql |
| **Kafka** | Async events, découpling | UserEventProducer, OutboxScheduler |

### Checklist de compréhension

- [ ] J'explique pourquoi hexagonal > monolith pour ce projet.
- [ ] Je trace un requête POST /users du client jusqu'à Kafka.
- [ ] J'explique l'Outbox pattern et ses avantages.
- [ ] Je peux écrire un unit test mockant les ports.
- [ ] Je peux écrire un integration test avec Testcontainers.
- [ ] Je comprends le rôle de JWT et du JwtWebFilter.
- [ ] Je peux impléter POST /tasks d'A à Z.

Fin du Deep-Dive. Prochaines étapes : parcourir les exercices (labs 1, 2, 3) et implémenter.
