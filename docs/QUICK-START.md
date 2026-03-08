# ⚡ Quick Start — 5 minutes pour commencer

Version 2026-03-08 | Pour les impatients

---

## 1️⃣ Cloner et naviguer (30 sec)

```powershell
cd C:\Users\Utilisateur\IdeaProjects
git clone https://github.com/DimitriChelloul/task-management-platform.git
cd task-management-platform
```

---

## 2️⃣ Vérifier les prérequis (1 min)

```powershell
# Java 21 ?
java -version
# Output: openjdk version "21.x.x"

# Maven ?
mvn -version
# Output: Maven 3.8+

# Docker ?
docker --version
# Output: Docker version ...
```

❌ Si l'une manque, consultez `INDEX.md` section "Prérequis".

---

## 3️⃣ Build du projet (2 min)

```powershell
mvn clean install
```

Vous devriez voir : `BUILD SUCCESS`

---

## 4️⃣ Lancer les services (1 min)

### Terminal 1 : User Service

```powershell
cd services\user-service
mvn spring-boot:run
```

Attendez : `Started UserServiceApplication`

### Terminal 2 : Task Service (VOTRE OBJECTIF)

```powershell
cd services\task-service
mvn spring-boot:run
```

À faire dans Lab 1 ! 😉

---

## 5️⃣ Premier test avec curl (30 sec)

```powershell
# Créer un utilisateur
curl -X POST http://localhost:8081/users `
  -H "Content-Type: application/json" `
  -d '{"username": "alice", "email": "alice@example.com", "password": "secret"}'

# Vous devriez voir :
# {"id": "...", "username": "alice", "email": "alice@example.com"}
```

✅ Si ça fonctionne, vous êtes prêt !

---

## 📖 Prochaines étapes

1. **Lire** : `docs/INDEX.md` (3 min)
2. **Lire** : `docs/deep-dive.md` sections 1-3 (20 min)
3. **Faire** : `docs/lab1-complete-implementation.md` étape 1-5 (1 h)

---

## 🆘 Erreur ?

Consultez `docs/faq-troubleshooting.md` section "Problèmes courants".

---

**C'est bon ? Allez dans `docs/INDEX.md` pour le vrai cours ! 🚀**
