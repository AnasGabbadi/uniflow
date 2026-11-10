# UniFlow

Application de gestion des étudiants, des formations et des inscriptions, réalisée dans le cadre du projet final DevOps.
L'objectif est de livrer une application fonctionnelle **et** une chaîne DevOps reproductible : Git, CI/CD, Docker, Kubernetes.

## Stack technique

| Besoin | Technologie |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot (Web, Data JPA, Validation, Actuator) |
| Base de données | PostgreSQL + Flyway |
| Tests | JUnit 5, tests d'API |
| Front-end | Interface simple (dossier `frontend/`) |
| Conteneurisation | Docker multi-stage + Docker Compose |
| Orchestration | Kubernetes local avec Minikube |
| CI/CD et qualité | GitHub Actions, SonarQube Cloud, Trivy |
| Gestion de projet | Jira (espace **IP**) |

## Structure du dépôt

```
uniflow/
├── backend/              # API Spring Boot (créée par IP-9)
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile        # (IP-11)
│   └── .dockerignore
├── frontend/             # Application front-end (créée par IP-16)
│   └── Dockerfile        # (IP-17)
├── kubernetes/           # Manifestes Kubernetes (sprint 3)
├── docs/
│   ├── backlog/          # Export du backlog Jira
│   ├── data-model/       # Diagramme de données (IP-12)
│   ├── uml/              # Diagrammes UML (IP-20)
│   ├── evidence/         # Captures et preuves (tests, pipelines, déploiements)
│   └── reports/          # Rapport technique final
├── .github/              # Workflows CI/CD et modèle de Pull Request
├── compose.yaml          # Lance back-end + front-end + PostgreSQL (IP-14)
├── .env.example          # Modèle des variables d'environnement (IP-13)
├── .gitignore
└── README.md
```

> Le back-end et le front-end sont séparés pour que chaque binôme travaille dans son dossier. Les fichiers demandés par le cahier des charges (`Dockerfile`, `.dockerignore`, `pom.xml`, `src/`) se trouvent dans `backend/`, et `compose.yaml` à la racine orchestre l'ensemble.

## Démarrage rapide

_À compléter au fil du Sprint 0 (IP-8)._

```bash
git clone https://github.com/<ORGANISATION>/uniflow.git
cd uniflow
cp .env.example .env      # puis renseigner les valeurs locales
docker compose up --build
```

## Règles de travail

Le workflow complet (cycle d'une fonctionnalité, revues, releases, hotfix) est décrit dans [docs/git-workflow.md](docs/git-workflow.md).

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