# Configuration du task-service

## 📋 Fichier : `task-service.yml`

Ce fichier contient la configuration complète du microservice Task.

### Structure

```yaml
server:
  port: 8083                        # Port d'écoute (différent du user-service)

spring:
  application:
    name: task-service             # Nom du service (pour Eureka)
  
  datasource:                       # Connexion à la BD PostgreSQL
    url: jdbc:postgresql://localhost:5432/task_db
    username: task_user
    password: task_password
  
  jpa:                              # Configuration Hibernate/JPA
    hibernate:
      ddl-auto: validate            # Flyway gère les migrations
  
  kafka:                            # Configuration Kafka
    bootstrap-servers: localhost:9092
    producer & consumer:            # Sérialisation JSON

kafka:
  topics:                           # Topics Kafka
    taskCreated: task.created
    taskCompleted: task.completed
    userCreated: user.created       # Listener pour les événements user

jwt:                                # Configuration JWT (partagée)
  secret: "0123456789abcdef0123456789abcdef"
  expiration-minutes: 120

eureka:                             # Service discovery
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

logging:                            # Niveaux de log
  level:
    org.dimitri.task: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## 🔧 Différences avec user-service.yml

| Aspect | user-service | task-service |
|--------|--------------|--------------|
| **Port** | 8081 | 8083 |
| **Base de données** | ❌ (JDBC) | ✅ PostgreSQL (JPA) |
| **Kafka topics** | `user.created` | `task.created`, `task.completed` |
| **Listeners Kafka** | ❌ | ✅ (`user.created`) |
| **Logging** | Standard | Détaillé (JPA + SQL) |

---

## 🚀 Utilisation

### En développement (localhost)

Le fichier est prêt à utiliser tel quel. Assurer que :

```bash
# 1. PostgreSQL est en cours d'exécution
docker-compose -f docker-compose.db.yml up

# 2. Kafka est en cours d'exécution
docker-compose -f docker-compose.kafka.yml up

# 3. Eureka est actif
# (lancer discovery-service)

# 4. Créer la base de données
createdb -U postgres task_db
```

### En production

Remplacer les valeurs :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<RDS_HOST>:5432/task_db
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS}

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL}

jwt:
  secret: ${JWT_SECRET}  # ⚠️ À stocker en variable d'env!
```

---

## 📚 Sections expliquées

### 1. Server configuration
```yaml
server:
  port: 8083  # Port unique pour task-service
```
- Doit être différent de user-service (8081)
- Doit être différent de api-gateway (8082)

### 2. Database (PostgreSQL)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/task_db
    username: task_user
    password: task_password
  
  jpa:
    hibernate:
      ddl-auto: validate  # NE pas utiliser create/update en prod!
```
- `validate` : Vérifie seulement le schéma
- Flyway gère les migrations SQL
- Hibernate génère les requêtes JPA

### 3. Kafka integration
```yaml
kafka:
  bootstrap-servers: localhost:9092  # Cluster Kafka local
  producer:
    value-serializer: JsonSerializer  # Sérialiser en JSON
  consumer:
    auto-offset-reset: earliest       # Rejouer les anciens messages
```

### 4. Kafka topics
```yaml
kafka:
  topics:
    taskCreated: task.created         # Publier quand tâche créée
    taskCompleted: task.completed     # Publier quand tâche complétée
    userCreated: user.created         # Écouter les users créés
```

### 5. JWT security
```yaml
jwt:
  secret: "0123456789abcdef0123456789abcdef"  # ⚠️ À changer!
  expiration-minutes: 120                      # 2 heures
```
- **DANGER** : Secret en dur = vulnérable!
- À stocker dans Azure Key Vault / AWS Secrets Manager en prod
- Doit être identique à user-service pour validation

### 6. Service discovery (Eureka)
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: task-service:${random.value}  # Unicité
```
- Enregistre automatiquement le service dans Eureka
- API Gateway peut découvrir task-service

### 7. Logging
```yaml
logging:
  level:
    org.dimitri.task: DEBUG          # Code custom
    org.hibernate.SQL: DEBUG         # Requêtes SQL
    org.springframework.web: DEBUG   # HTTP requests
```

### 8. Management endpoints (optionnel)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```
- `/actuator/health` : État du service
- `/actuator/metrics` : Métriques JVM

---

## 🔐 Sécurité — IMPORTANT

### ⚠️ Secrets à ne JAMAIS hardcoder

**À FAIRE** :
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}  # Lire depuis env var

jwt:
  secret: ${JWT_SECRET}       # Lire depuis Azure Key Vault
```

**À NE PAS FAIRE** :
```yaml
# ❌ JAMAIS ceci !
jwt:
  secret: "mon-super-secret-hardcoded"
```

### Configuration pour Azure
```yaml
# En production sur Azure
spring:
  cloud:
    azure:
      keyvault:
        secret:
          property-name: jwt.secret  # Lire depuis Key Vault
```

---

## 🧪 Tests

### Vérifier que le service est enregistré dans Eureka
```bash
curl http://localhost:8761/eureka/apps/task-service
```

Devrait retourner les instances actives du task-service.

### Vérifier la santé du service
```bash
curl http://localhost:8083/actuator/health
```

Devrait retourner `{"status":"UP"}`.

---

## 📝 Checklist de déploiement

Avant de déployer, assurer :

- [ ] `datasource.password` est en variable d'environnement
- [ ] `jwt.secret` est en Azure Key Vault
- [ ] `kafka.bootstrap-servers` pointe sur le bon cluster
- [ ] `eureka.client.service-url` pointe sur Eureka
- [ ] `server.port` est unique (8083)
- [ ] Les migrations Flyway sont appliquées
- [ ] Les logs sont au niveau INFO (pas DEBUG) en prod

---

## 🔗 Fichiers liés

- `user-service.yml` — Configuration du service utilisateur
- `discovery-service.yml` — Configuration d'Eureka
- `application.yml` — Configuration API Gateway
- `docker-compose.db.yml` — PostgreSQL
- `docker-compose.kafka.yml` — Kafka

---

**Version** : 1.0.0
**Date** : 2026-03-08
