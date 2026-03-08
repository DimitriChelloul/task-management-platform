# 📚 Index complet — Task Management Platform Course

Version 2026-03-08 | Guide de navigation pour la formation

---

## 🎯 Démarrer ici

Bienvenue ! Ce projet est un **cours complet** sur les microservices avec Spring Boot, Kafka, JWT, et l'architecture hexagonale.

### Pour qui ?

- ✅ Développeurs Java intermédiaires voulant apprendre les microservices
- ✅ Étudiants en informatique cherchant un projet réaliste
- ✅ Équipes voulant comprendre l'architecture d'une plateforme moderne

### Combien de temps ?

- **Jour 1** : Setup + Deep-dive architecture (3-4 heures)
- **Jour 2** : Lab 1 (POST /tasks en mémoire) (2-3 heures)
- **Jour 3** : Lab 2 (JWT + sécurité) (2-3 heures)
- **Jour 4** : Lab 3 (JPA + Postgres + Testcontainers) (4-5 heures)
- **Bonus** : CI/CD + Docker (2-3 heures)

---

## 📖 Structure pédagogique

```
├─ [THÉORIE] Deep-dive complet
│  └─ Comprendre comment ça fonctionne en détail
│
├─ [PRATIQUE] Labs (exercices guidés)
│  ├─ Lab 1 : POST /tasks en mémoire
│  ├─ Lab 2 : Ajouter JWT
│  └─ Lab 3 : JPA + Postgres + Testcontainers
│
├─ [RÉFÉRENCE] Solutions prêtes à copier-coller
│
├─ [SUPPORT] FAQ & Troubleshooting
│
└─ [RESSOURCES] Architecture diagrams, glossaire, liens utiles
```

---

## 🗂️ Index des fichiers de documentation

| Fichier | Contenu | Durée |
|---------|---------|-------|
| **deep-dive.md** | Explication complète (ligne par ligne) de l'architecture hexagonale, OutBox pattern, JWT, Kafka, et code source commenté | 45 min |
| **lab1-complete-implementation.md** | Exercice guidé pas à pas : implémenter POST /tasks en mémoire (11 étapes) | 2-3 h |
| **solutions-lab1-lab2.md** | Code complet prêt à copier-coller pour Lab 1 et Lab 2 | 15 min |
| **faq-troubleshooting.md** | Questions fréquentes et solutions aux erreurs courantes | 30 min (reference) |
| **project-presentation.md** (original) | Présentation générale du projet | 10 min |
| **project-presentation-detailed.md** | Version étendue avec architecture + concepts | 20 min |
| **course-handbook.md** (généré CI) | Compilation PDF de tous les cours (auto-généré sur GitHub) | 60 min |

---

## 🚀 Parcours recommandé (pour débuter)

### Phase 1 : Compréhension (30 min)

1. Lire **project-presentation.md** (overview)
2. Regarder l'architecture dans **project-presentation-detailed.md**
3. Lire les 3 premières sections de **deep-dive.md** (architecture + domain + ports)

### Phase 2 : Apprentissage théorique (1 heure)

4. Lire le reste de **deep-dive.md** (code annoté, testing, Outbox)
5. Consulter **faq-troubleshooting.md** sections 1-2 (ports, JWT)

### Phase 3 : Hands-on Lab 1 (2-3 heures)

6. Suivre **lab1-complete-implementation.md** étape par étape
7. Créer les fichiers Java dans votre IDE
8. Exécuter les tests : `mvn test`
9. Tester manuellement avec curl

### Phase 4 : Hands-on Lab 2 (2-3 heures)

10. Ajouter JWT au TaskController (voir **solutions-lab1-lab2.md**)
11. Tester avec un token JWT (voir **faq-troubleshooting.md** Q2.1)

### Phase 5 : Bonus — Lab 3 (4-5 heures)

12. Remplacer InMemory par JPA + Postgres (non encore documenté, c'est votre challenge !)
13. Utiliser Testcontainers pour les tests (voir exemple dans **lab1-complete-implementation.md**)

---

## 🛠️ Prérequis

Avant de commencer, assurez-vous d'avoir :

- ✅ **JDK 21** (LTS)
  ```bash
  java -version
  # Doit afficher java 21.x.x
  ```

- ✅ **Maven 3.8+**
  ```bash
  mvn -version
  ```

- ✅ **Git**
  ```bash
  git --version
  ```

- ✅ **Docker** (pour Lab 3 + Testcontainers)
  ```bash
  docker --version
  ```

- ✅ **IDE** (IntelliJ IDEA, VS Code, ou Eclipse)

### Installation rapide (Windows PowerShell)

```powershell
# Vérifier/installer via Chocolatey
choco install openjdk21 maven docker-desktop git

# Ou télécharger manuellement
# JDK 21: https://www.oracle.com/java/technologies/downloads/
# Maven:  https://maven.apache.org/download.cgi
# Docker: https://www.docker.com/products/docker-desktop
# Git:    https://git-scm.com/
```

---

## 🏗️ Architecture du projet (résumé)

```
Task Management Platform
│
├── Infrastructure (Découverte, Config, API Gateway)
│   ├── discovery-service (Eureka)
│   ├── config-service (Spring Cloud Config)
│   └── api-gateway (Spring Cloud Gateway + JWT)
│
├── Services (Microservices business)
│   ├── user-service (COMPLET)
│   │   ├── Créer des utilisateurs
│   │   ├── Authentifier (JWT)
│   │   ├── Outbox pattern + Kafka
│   │   └── JDBC persistence
│   │
│   └── task-service (À IMPLÉMENTER)
│       ├── Créer des tâches (Lab 1)
│       ├── Ajouter JWT (Lab 2)
│       └── Ajouter JPA + Postgres (Lab 3)
│
├── Frontend
│   └── React app (http://localhost:3000)
│
└── Documentation (Vous êtes ici !)
    ├── Théorie (deep-dive)
    ├── Pratique (labs)
    └── Support (FAQ)
```

---

## 📚 Concepts clés du cours

Vous apprendrez :

1. **Architecture Hexagonale** — Découpler le domaine de l'infrastructure
2. **Ports & Adapters** — Interfaces flexibles et testables
3. **Microservices** — Services indépendants communiquant via API
4. **Kafka + Outbox** — Garantir la cohérence des événements distribués
5. **JWT Security** — Authentification stateless
6. **Spring Data & JPA** — ORM pour persister en base de données
7. **Testing** — Unit tests avec mocks, integration tests avec Testcontainers
8. **CI/CD** — GitHub Actions pour automatiser le déploiement

---

## 🎓 Qu'allez-vous faire ?

### Lab 1 — Créer une API Task en mémoire

**Objectif** : Implémenter POST /tasks en suivant l'architecture hexagonale

**Avant** :
```bash
POST /tasks
→ 404 Not Found
```

**Après** :
```bash
POST /tasks {"title": "Buy milk"}
→ 201 Created
{
  "id": "550e8400-...",
  "title": "Buy milk",
  "done": false,
  "createdAt": "2026-03-08T15:30:00Z"
}
```

---

### Lab 2 — Ajouter JWT

**Objectif** : Protéger les endpoints avec JWT

**Avant** :
```bash
curl -X POST /tasks {"title": "..."}
→ 201 Created (pas de sécurité)
```

**Après** :
```bash
# Sans token
curl -X POST /tasks {"title": "..."}
→ 401 Unauthorized

# Avec token valide
curl -X POST /tasks \
  -H "Authorization: Bearer eyJ..." \
  {"title": "..."}
→ 201 Created
```

---

### Lab 3 — Remplacer par JPA + Postgres

**Objectif** : Persister les données dans une vraie base de données

**Avant** :
```
Données en mémoire (perdues au redémarrage)
```

**Après** :
```
Table SQL "tasks" avec:
- id (UUID)
- title (VARCHAR)
- description (TEXT)
- done (BOOLEAN)
- created_at (TIMESTAMP)

Données persistées même après redémarrage !
```

---

## 💻 Commandes essentielles

### Build du projet

```bash
cd C:\Users\Utilisateur\IdeaProjects\task-management-platform
mvn clean install
```

### Lancer user-service

```bash
cd services/user-service
mvn spring-boot:run
# http://localhost:8081
```

### Lancer task-service

```bash
cd services/task-service
mvn spring-boot:run
# http://localhost:8083
```

### Lancer les tests

```bash
cd services/task-service
mvn test
```

### Docker Compose (BD + Kafka)

```bash
docker-compose -f docker-compose.db.yml up
docker-compose -f docker-compose.kafka.yml up
```

---

## 🔗 Ressources externes

### Documentation officielle

- [Spring Boot 3.4 docs](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Kafka documentation](https://kafka.apache.org/documentation/)
- [JUnit 5](https://junit.org/junit5/)
- [Testcontainers](https://www.testcontainers.org/)

### Livres recommandés

- "Clean Architecture" — Robert C. Martin
- "Microservices Patterns" — Chris Richardson
- "Spring in Action" — Craig Walls

### Tutoriels YouTube

- Spring Boot Microservices (Spring Boot Official)
- JUnit 5 & Testcontainers (Baeldung)
- Docker & Kubernetes for Java (Nigel Poulton)

---

## 📞 Support & Communauté

Vous êtes bloqué ?

1. **Consulter FAQ** → `faq-troubleshooting.md`
2. **Google the error** → 95% du temps, quelqu'un a eu le même problème
3. **Stack Overflow** → Tag `spring-boot`, `java`, `docker`
4. **GitHub Issues** → Dans ce repository

---

## 🎯 Checklist finale

Avant de terminer le cours, vérifiez :

- [ ] Je comprends la différence entre Port et Adapter
- [ ] Je peux expliquer le pattern Outbox et ses avantages
- [ ] J'ai implémenté Lab 1 complètement
- [ ] J'ai ajouté JWT à Lab 2
- [ ] J'ai testé avec Testcontainers
- [ ] Les tests passent : `mvn test -q`
- [ ] Je peux tracer une requête HTTP du client jusqu'à la BD

---

## 📅 Roadmap future

Fonctionnalités à ajouter après le cours :

- [ ] Lab 3 : JPA + Postgres pour task-service
- [ ] Event sourcing (enregistrer tous les changements)
- [ ] CQRS pattern (séparer read et write)
- [ ] Tracing distribué (Jaeger)
- [ ] Métriques (Prometheus)
- [ ] GraphQL au lieu de REST
- [ ] API Documentation (Swagger/OpenAPI)
- [ ] Containerisation & Kubernetes

---

## 📝 Notes & Observations

**Dernière mise à jour** : 2026-03-08
**Version** : 1.0.0
**Auteur** : Dimitri Chelloul
**Langage** : Français
**Status** : 🟢 Production ready

---

## 🎓 Comment utiliser ce cours

### Pour les étudiants

1. Lire les théories d'abord
2. Faire les labs **sans copier-coller** (essayer seul d'abord !)
3. Comparer avec les solutions
4. Relire les sections mal comprises
5. Discuter avec des camarades

### Pour les instructeurs

1. Adapter les labs au niveau des étudiants
2. Ajouter d'autres exercices (par ex: Lab 4 - Outbox + Kafka)
3. Utiliser les solutions pour corriger
4. Enregistrer les cours vidéo basés sur le deep-dive

### Pour les professionnels

1. Utiliser comme référence d'architecture
2. Adapter aux besoins de votre entreprise
3. Intégrer à une formation interne
4. Contribuer des améliorations

---

## 🚀 Prochaines étapes

**Maintenant** :
1. Installer les prérequis ✅
2. Lire **project-presentation.md** ✅
3. Lire **deep-dive.md** sections 1-3 ✅

**Demain** :
4. Commencer **Lab 1** ✅
5. Créer les fichiers Task, TaskWritePort, InMemoryTaskRepository
6. Créer le controller et tester

Bonne chance ! 🎉

---

**Questions ?** Consultez `faq-troubleshooting.md` ou créez une GitHub Issue.

Fin de l'Index.
