# Back-end UniFlow

API REST de gestion des étudiants, des formations et des inscriptions.

**Stack :** Java 21 · Spring Boot · Maven · Spring Data JPA · PostgreSQL · Flyway · Spring Validation · Spring Boot Actuator · springdoc (Swagger UI)

## Sommaire

1. [Prérequis](#prérequis)
2. [Structure du projet](#structure-du-projet)
3. [Configuration](#configuration)
4. [Lancer l'application](#lancer-lapplication)
5. [Base de données et migrations](#base-de-données-et-migrations)
6. [API](#api)
7. [Tests et couverture](#tests-et-couverture)
8. [Conventions d'équipe](#conventions-déquipe)

## Prérequis

| Outil | Version | Usage |
|---|---|---|
| JDK | 21 | Compilation et exécution (hors Docker) |
| Docker Desktop | récent | Base PostgreSQL et image de l'API |
| Git | récent | Versionnement |

Maven n'est pas à installer : le wrapper `mvnw` / `mvnw.cmd` du dépôt s'en charge.

## Structure du projet

Organisation **par fonctionnalité** : chaque module métier contient ses propres couches.

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/isga/uniflow/
│   │   │   ├── UniFlowApplication.java
│   │   │   ├── config/            Configuration globale (sécurité, CORS, OpenAPI)
│   │   │   ├── common/            Composants partagés par tous les modules
│   │   │   │   ├── exception/     Exceptions communes (BusinessException...)
│   │   │   │   ├── handler/       GlobalExceptionHandler (@RestControllerAdvice)
│   │   │   │   └── response/      Format standard des erreurs (ApiErrorResponse)
│   │   │   ├── etudiant/          controller, service, repository, entity, dto, exception
│   │   │   ├── formation/         controller, service, repository, entity, dto, exception
│   │   │   └── inscription/       controller, service, repository, entity, dto, exception
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/      Migrations Flyway (V1__..., V2__...)
│   └── test/java/com/isga/uniflow/
│       ├── etudiant/
│       ├── formation/
│       └── inscription/
├── Dockerfile                     Image multi-stage de l'API
├── .dockerignore
└── pom.xml
```

### Rôle de chaque couche

| Package | Responsabilité |
|---|---|
| `controller` | Reçoit les requêtes HTTP et renvoie les réponses. Aucune logique métier. |
| `service` | Logique métier et conversion DTO <-> entité. |
| `repository` | Accès aux données via Spring Data JPA. |
| `entity` | Entités JPA : une classe par table, sans logique métier. |
| `dto` | Objets échangés par l'API (requêtes et réponses), avec les règles de validation. |
| `exception` | Exceptions et gestion d'erreurs propres au module. |

### Règles à respecter

- Un module n'accède **jamais** directement au repository d'un autre module : il passe par son service.
- Les entités ne sont **jamais** renvoyées telles quelles par l'API : on renvoie un DTO.
- La validation se déclare sur les DTO de requête (`@NotBlank`, `@Positive`...), avec `@Valid` dans le contrôleur.
- Le `package` Java doit toujours correspondre au dossier du fichier.
- Les composants vraiment partagés vont dans `common/`, le reste reste dans son module.

## Configuration

Aucun secret n'est écrit dans le code ni dans `application.properties` : tout vient de variables d'environnement.

| Variable | Rôle | Valeur par défaut |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/uniflow` |
| `DB_USERNAME` | Utilisateur de la base | `uniflow` |
| `DB_PASSWORD` | Mot de passe de la base | **aucune** (obligatoire) |
| `SERVER_PORT` | Port HTTP de l'API | `8080` |

`DB_PASSWORD` n'a volontairement pas de valeur par défaut : l'application refuse de démarrer sans lui.

Les valeurs se définissent dans un fichier `.env` **à la racine du dépôt** (à côté de `compose.yaml`), créé à partir du modèle `.env.example` :

```powershell
Copy-Item .env.example .env
```

Le fichier `.env` est ignoré par Git. Ne jamais le commiter, ne jamais y laisser de mot de passe réel dans `.env.example`.

> Spring Boot ne lit pas les fichiers `.env`. Docker Compose, lui, les lit et transmet les valeurs à l'application. Hors Docker, définir les variables dans le terminal (voir ci-dessous).

## Lancer l'application

### Avec Docker Compose (recommandé)

Depuis la **racine du dépôt** :

```powershell
docker compose up --build
```

Cela démarre PostgreSQL, attend qu'il soit prêt, puis lance l'API sur `http://localhost:8080`. Les données sont conservées dans un volume Docker.

```powershell
docker compose down        # arrête les conteneurs, garde les données
docker compose down -v     # arrête ET supprime les données
docker compose logs -f app # suit les logs de l'API
```

### Sans Docker pour l'API (développement)

1. Démarrer PostgreSQL seul (le port 5432 doit être publié dans `compose.yaml`) :
   ```powershell
   docker compose up -d db
   ```
2. Lancer l'API depuis `backend/` :
   ```powershell
   $env:DB_PASSWORD = "votre_mot_de_passe"
   .\mvnw.cmd spring-boot:run
   ```
   Sous Linux ou macOS : `DB_PASSWORD=... ./mvnw spring-boot:run`.


## Base de données et migrations

Le schéma est géré **uniquement par Flyway**. Hibernate se contente de le valider (`spring.jpa.hibernate.ddl-auto=validate`) : il ne crée ni ne modifie aucune table.

Les migrations sont exécutées automatiquement au démarrage, avant JPA.

| Fichier | Contenu |
|---|---|
| `V1__init.sql` | Table `etudiant` |
| `V2__seed_dev.sql` | Table `formation` |
  `V3__ajouter_description_et_dates_formation.sql` | Table `modifier formation` |



### Règles

- Nom : `V<numéro>__<description>.sql`, avec **deux** underscores.
- **Ne jamais modifier une migration déjà fusionnée** : Flyway en mémorise l'empreinte et refuse de démarrer si elle change. Pour évoluer, ajouter une nouvelle migration avec le numéro suivant.
- **Numéros uniques** : avant de créer une migration, récupérer `develop` et vérifier le dernier numéro utilisé pour éviter une collision avec un coéquipier.
- Contraintes (clés primaires, clés étrangères, `UNIQUE`, `NOT NULL`, `CHECK`) déclarées dans le SQL : c'est la base qui garantit l'intégrité.
- L'historique des migrations appliquées est dans la table `flyway_schema_history`.

En développement, si le schéma local est dans un état incohérent, repartir d'une base vide : `docker compose down -v`, puis `docker compose up --build`.

Le modèle de données est documenté dans `docs/data-model/`.

## API

Réponses JSON. Les dates utilisent le format `yyyy-MM-dd`.

### Formations

| Méthode | URL | Description | Réponses |
|---|---|---|---|
| `POST` | `/api/formations` | Crée une formation | `201`, `400`, `409` |

Exemple :

```http
POST /api/formations
Content-Type: application/json

{
  "intitule": "Java avancé",
  "description": "Spring Boot et JPA",
  "duree": 40,
  "capaciteMax": 20,
  "dateDebut": "2026-12-01",
  "dateFin": "2026-12-20"
}
```

Règles de validation : intitulé, durée, capacité et dates obligatoires ; durée et capacité strictement positives ; la date de fin ne peut pas précéder la date de début ; l'intitulé est unique (sans tenir compte de la casse). La description est facultative.

### Codes de réponse

| Code | Signification |
|---|---|
| `201 Created` | Ressource créée, avec l'en-tête `Location` |
| `400 Bad Request` | Données invalides ou JSON illisible |
| `409 Conflict` | Conflit métier (par exemple intitulé déjà utilisé) |

Exemple d'erreur `400` :

```json
{
  "timestamp": "2026-11-17T10:00:00Z",
  "status": 400,
  "message": "Données invalides",
  "erreurs": { "capaciteMax": "La capacité doit être strictement positive" }
}
```

> Les modules `etudiant` et `inscription` seront documentés ici au fur et à mesure de leur livraison.

## Tests et couverture

```powershell
cd backend
.\mvnw.cmd clean verify
```

La commande compile, exécute les tests et génère le rapport de couverture JaCoCo : `target/site/jacoco/index.html` (et `jacoco.xml`, lu par SonarQube).

- Les tests unitaires (validation, services avec Mockito) ne demandent aucune base.
- Le test `contextLoads` démarre toute l'application : il nécessite PostgreSQL et la variable `DB_PASSWORD`.
- La CI doit exécuter `./mvnw verify` (et non `test` ou `package`), sinon le rapport de couverture n'est pas produit.
- Les packages sans logique (`entity`, `config`, `dto`, classe `*Application`) sont exclus du calcul de couverture dans `pom.xml` (`sonar.coverage.exclusions`). Ils restent analysés pour les bugs et la sécurité.

Les tests reprennent l'arborescence du code : `src/test/java/com/isga/uniflow/<module>/...`.

## Conventions d'équipe

- **Branches :** `feature/<TICKET>-<description-courte>`, créées depuis `develop`. Exemple : `feature/IP-12-creation-formation`.
- **Fusion :** par pull request, relue par un coéquipier. Pas de commit direct sur `develop` ni `main`.
- **Commits :** message court et explicite, au présent. Exemple : `IP-12 feat: créer une formation`.
- **Commentaires :** chaque classe a un commentaire Javadoc qui explique son rôle ; commenter le *pourquoi* plus que le *comment*.
- **Secrets :** jamais dans Git. Voir `.env.example` et `.gitignore`.