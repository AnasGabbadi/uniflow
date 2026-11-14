# UniFlow

[![CI](https://github.com/AnasGabbadi/uniflow/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/AnasGabbadi/uniflow/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=AnasGabbadi_uniflow&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AnasGabbadi_uniflow)

Application de gestion des étudiants, des formations et des inscriptions, réalisée dans le cadre du projet final DevOps.
L'objectif est de livrer une application fonctionnelle **et** une chaîne DevOps reproductible : Git, CI/CD, Docker, Kubernetes.

## Stack technique

| Besoin | Technologie |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot (Web, Data JPA, Validation, Actuator) |
| Base de données | PostgreSQL + Flyway |
| Tests | JUnit 5, JaCoCo, tests d'API |
| Front-end | Angular + SCSS (dossier `frontend/`) |
| Conteneurisation | Docker multi-stage + Docker Compose |
| Orchestration | Kubernetes local avec Minikube |
| CI/CD et qualité | GitHub Actions, SonarQube Cloud, Trivy |
| Gestion de projet | Jira (espace **IP**) |

## Structure du dépôt

```
uniflow/
├── backend/              # API Spring Boot
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile        # Image multi-stage de l'API (IP-11)
│   └── .dockerignore
├── frontend/             # Application Angular (IP-16)
│   └── Dockerfile        # Image Nginx du front (IP-17)
├── kubernetes/           # Manifestes Kubernetes (sprint 3)
├── docs/
│   ├── backlog/          # Export du backlog Jira
│   ├── data-model/       # Diagramme de données (IP-12)
│   ├── uml/              # Diagrammes UML (IP-20)
│   ├── evidence/         # Captures et preuves (tests, pipelines, déploiements)
│   └── reports/          # Rapport technique final
├── .github/              # Workflow CI et modèle de Pull Request
├── compose.yaml          # Lance l'API et PostgreSQL (IP-14), puis le front (IP-17)
├── .env.example          # Modèle des variables d'environnement (IP-13)
├── .gitignore
└── README.md
```

> Le back-end et le front-end sont séparés pour que chaque binôme travaille dans son dossier. Les fichiers demandés par le cahier des charges (`Dockerfile`, `.dockerignore`, `pom.xml`, `src/`) se trouvent dans `backend/`, et `compose.yaml` à la racine orchestre l'ensemble.

## Prérequis

| Outil | Version | Vérification |
|---|---|---|
| Git | récent | `git --version` |
| Docker Desktop | récent, **lancé** | `docker --version` |
| JDK | 21 ou plus récent (le code cible Java 21) — seulement pour développer hors Docker | `java -version` |
| Node.js | 20.19 ou plus récent — seulement pour développer le front | `node -v` |

Maven n'a pas besoin d'être installé : le projet utilise le Maven Wrapper (`./mvnw`).
Sous Windows, utilisez **Git Bash** pour lancer les commandes ci-dessous.

## Démarrage rapide

### 1. Récupérer le projet

```bash
git clone https://github.com/AnasGabbadi/uniflow.git
cd uniflow
git switch develop
```

### 2. Configurer l'environnement

```bash
cp .env.example .env
```

Dans `.env`, remplacer les deux `change_me` par **le même** mot de passe (`POSTGRES_PASSWORD` et `DB_PASSWORD`).
Le fichier `.env` est ignoré par Git : il ne doit jamais être commité.

| Variable | Obligatoire | Rôle |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | oui | Création de la base PostgreSQL |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | oui | Connexion de l'API à la base (hôte `db` dans Docker) |
| `SERVER_PORT` | non (`8080`) | Port de l'API dans le conteneur |
| `APP_PORT` | non (`8080`) | Port de l'API sur votre PC |
| `APP_IMAGE` | non (`uniflow:latest`) | Nom de l'image Docker de l'API |

### 3. Lancer l'API et PostgreSQL

```bash
docker compose up --build -d
docker compose ps        # db : healthy, app : Up
```

Vérification : http://localhost:8080/actuator/health doit répondre `{"status":"UP"}`.

| Action | Commande |
|---|---|
| Voir les logs de l'API | `docker compose logs -f app` |
| Arrêter (données conservées) | `docker compose down` |
| Tout réinitialiser (supprime la base) | `docker compose down -v` |

### 4. Lancer le front-end (développement)

Dans un second terminal :

```bash
cd frontend
npm install
npm start
```

Ouvrir http://localhost:4200. Les appels vers `/api` sont redirigés vers l'API par `proxy.conf.json`.

### Développer le back-end hors Docker

Pour déboguer l'API depuis l'IDE, démarrer seulement la base, puis l'application :

```bash
docker compose up -d db
cd backend
DB_URL=jdbc:postgresql://localhost:5432/uniflow DB_PASSWORD=<votre mot de passe> ./mvnw spring-boot:run
```

> La base n'est pas exposée par défaut. Pour y accéder depuis votre PC, ajouter temporairement `ports: ["5432:5432"]` au service `db` (sans le commiter).

## Tests et qualité

| Action | Commande |
|---|---|
| Tests back-end + rapport de couverture JaCoCo | `cd backend && ./mvnw verify` (base démarrée) |
| Lint front-end | `cd frontend && npm run lint` |
| Format front-end | `cd frontend && npm run format:check` |
| Build front-end | `cd frontend && npm run build` |

À chaque Pull Request, la CI GitHub Actions compile, teste et analyse le code avec SonarQube Cloud.
La fusion est bloquée si la CI ou la Quality Gate échoue.

## Problèmes fréquents

| Symptôme | Cause | Solution |
|---|---|---|
| `password authentication failed for user "uniflow"` | La base a été créée avec un ancien mot de passe (le volume le conserve) | `docker compose down -v` puis `docker compose up --build -d` |
| `DB_PASSWORD manquant dans le fichier .env` | Fichier `.env` absent ou incomplet | `cp .env.example .env` puis compléter |
| `port is already allocated` sur 8080 | Un autre programme utilise le port | Changer `APP_PORT` dans `.env` |
| `curl: (52) Empty reply from server` | L'API redémarre en boucle | Lire `docker compose logs app` |
| `./mvnw: Permission denied` | Droit d'exécution perdu sous Windows | `chmod +x backend/mvnw` |

## Règles de travail

- Workflow Git complet (cycle d'une fonctionnalité, revues, releases, hotfix) : [docs/git-workflow.md](docs/git-workflow.md)
- Quand un ticket est-il terminé ? [docs/definition-of-done.md](docs/definition-of-done.md)

### Branches
- `main` : versions stables et livrées (protégée).
- `develop` : intégration des fonctionnalités (protégée).
- `feature/IP-<n>-<description>` : une branche par ticket Jira, créée depuis `develop`.
- `hotfix/IP-<n>-<description>` : correction urgente, créée depuis `main`.

Exemple : `feature/IP-23-ajout-etudiant`

### Commits
Format : `IP-<n> <type>: <message>`, avec les types `feat`, `fix`, `docs`, `test`, `chore`, `ci`, `refactor`.

Exemple : `IP-23 feat: ajouter la création d'un étudiant`

### Pull Requests
- Toujours vers `develop` (sauf release et hotfix vers `main`).
- Titre avec la clé Jira, au moins **une approbation** d'un autre membre.
- Fusion uniquement si la CI et la Quality Gate SonarQube sont vertes.

### Sécurité
- Ne **jamais** versionner de mot de passe, de token ou de fichier `.env`.
- Les secrets CI sont stockés dans **Settings → Secrets and variables → Actions**.