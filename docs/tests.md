# Stratégie de test (back-end et front-end)

Ce document décrit la stratégie de test de l'API UniFlow, la base de données utilisée pendant les tests et la façon de les lancer.

## 1. Stratégie : trois niveaux de tests

| Niveau | Outils | Base de données | Ce qui est vérifié | Exemples |
|---|---|---|---|---|
| **Unitaire** | JUnit 5, Mockito, MockMvc standalone | aucune (repositories simulés) | Règles métier des services, codes HTTP des contrôleurs, validation des DTO, gestionnaire d'erreurs | `FormationServiceTest`, `EtudiantControllerTest`, `FormationRequestValidationTest` |
| **Repository** | `@DataJpaTest` + Testcontainers | **PostgreSQL 16 réel** | Mapping JPA, contraintes SQL (unicité, clés étrangères, `CHECK`, `ON DELETE CASCADE`), requêtes personnalisées | `EtudiantRepositoryIntegrationTest`, `FormationRepositoryIntegrationTest`, `InscriptionRepositoryIntegrationTest` |
| **Intégration (bout en bout)** | `@SpringBootTest` + MockMvc + Testcontainers | **PostgreSQL 16 réel** | Contrôleur → service → repository → base, sans aucun mock | `InscriptionFlowIntegrationTest`, `UniflowApplicationTests` |

Les tests endpoint par endpoint sont des tests unitaires de contrôleur (IP-47). Les tests d'intégration ne les dupliquent pas : ils vérifient seulement que les couches fonctionnent ensemble.

Convention de nommage : les classes `*IntegrationTest` démarrent une base. Elles sont exécutées par Surefire, comme les `*Test`, avec un simple `./mvnw verify`.

### Ce que couvrent les tests de repository et d'intégration

| Règle | Test |
|---|---|
| E-mail étudiant unique (`uq_etudiant_email`) | `EtudiantRepositoryIntegrationTest.emailEnDoubleEstRefuseParLaBase` |
| Unicité de l'e-mail sensible à la casse en base, contrôle insensible à la casse assuré par le service (`existsByEmailIgnoreCase`) | `…laBaseEstSensibleALaCasseEtLeRepositoryDetecteLeDoublonSansCasse` |
| Intitulé de formation unique (`uq_formation_intitule`) | `FormationRepositoryIntegrationTest.intituleEnDoubleEstRefuseParLaBase` |
| `CHECK` sur les dates (V3), la durée, la capacité et le statut de formation | `…dateDeFinAvantLaDateDeDebutEstRefusee`, `…dureeNulleEstRefusee`, etc. |
| Comptage des inscriptions actives (`EN_ATTENTE` et `VALIDEE`, pas `ANNULEE`) | `…compteLesInscriptionsActivesEnAttenteEtValideesMaisPasAnnulees`, `…ParFormation…` |
| Couple étudiant/formation unique (`uq_inscription_etudiant_formation`) | `InscriptionRepositoryIntegrationTest.memeEtudiantNePeutPasSInscrireDeuxFoisALaMemeFormation` |
| Clés étrangères `fk_inscription_etudiant` et `fk_inscription_formation` | `…inscriptionVersUnEtudiantInexistantEstRefusee`, `…UneFormationInexistante…` |
| `ON DELETE CASCADE` de la base (SQL natif, sans JPA) | `…supprimerUneFormationEnSqlSupprimeSesInscriptionsEnCascade`, `…UnEtudiantEnSql…` |
| `cascade = REMOVE` côté JPA | `…supprimerUneFormationViaJpaSupprimeSesInscriptions`, `…UnEtudiantViaJpa…` |
| `JOIN FETCH` sans `LazyInitializationException` | `…findAllAvecDetailsChargeEtudiantEtFormationSansRequeteParesseuse` |
| Inscription, puis places restantes décrémentées (API complète) | `InscriptionFlowIntegrationTest.inscrireUnEtudiantDecrementeLesPlacesRestantesDeLaFormation` |
| Double inscription → 409 `INSCRIPTION_EN_DOUBLE` (Problem Details) | `InscriptionFlowIntegrationTest.inscrireDeuxFoisLeMemeEtudiantRenvoie409AuFormatProblemDetails` |

## 2. La base de test : Testcontainers

Les tests de repository et d'intégration tournent sur un **vrai PostgreSQL 16** (`postgres:16-alpine`, même image que `compose.yaml`), démarré automatiquement dans Docker par [Testcontainers](https://java.testcontainers.org/). On n'utilise pas H2 : les contraintes, les requêtes SQL natives et les migrations Flyway sont testées sur le moteur réellement utilisé en production.

Fonctionnement, dans [`TestcontainersConfiguration`](../backend/src/test/java/com/isga/uniflow/TestcontainersConfiguration.java) :

1. La classe de test déclare `@Import(TestcontainersConfiguration.class)`.
2. Testcontainers démarre un conteneur PostgreSQL sur un **port aléatoire**, ce qui évite tout conflit avec une base existante sur `5432`.
3. `@ServiceConnection` transmet l'URL, l'utilisateur et le mot de passe du conteneur à Spring Boot. Les variables `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` **ne sont pas nécessaires** pour les tests.
4. Flyway applique les migrations V1 à V3 sur la base vierge.
5. Le conteneur est un champ `static` : **un seul conteneur** sert à toute l'exécution Maven, même si plusieurs contextes Spring sont créés.
6. À la fin de la JVM, le conteneur compagnon **Ryuk** (`testcontainers/ryuk`) supprime le conteneur PostgreSQL. Rien ne reste dans Docker.

Les tests **ne dépendent plus** de la base de la stack Compose, ni du conteneur manuel `uniflow-db` sur `localhost:5432`.

Dépendances de test ajoutées au `pom.xml` (versions gérées par le BOM Spring Boot 4.1.2, avec Testcontainers 2.0.5) :

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-testcontainers</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers-postgresql</artifactId>   <!-- nom 2.x (anciennement "postgresql") -->
  <scope>test</scope>
</dependency>
```

### Données de test

- Flyway applique aussi `V2__seed_dev.sql` : la base de test contient donc les données du seed.
- Les tests `@DataJpaTest` sont **annulés** (rollback) à la fin de chaque test.
- Les tests `@SpringBootTest` + MockMvc **ne sont pas annulés** : chaque requête HTTP a sa propre transaction.

Règles à respecter quand on écrit un test :

- créer ses propres données avec des valeurs **uniques**, grâce à [`TestData`](../backend/src/test/java/com/isga/uniflow/TestData.java) (suffixe UUID) ;
- ne **jamais** utiliser les données du seed ;
- ne **jamais** asserter sur des totaux globaux (`count()`, taille de `findAll()`, nombre de lignes de `compterInscriptionsActivesParFormation`) : vérifier uniquement les valeurs des identifiants créés par le test.

## 3. Prérequis

| Outil | Remarque |
|---|---|
| **Docker Desktop lancé** | Indispensable : sans Docker, les tests `*IntegrationTest` et `UniflowApplicationTests` échouent. |
| JDK 21 | Maven n'est pas nécessaire : utiliser le Maven Wrapper `./mvnw`. |
| Accès à Docker Hub (premier lancement) | Pour télécharger `postgres:16-alpine` et `testcontainers/ryuk`. Ensuite, les images sont en cache. |

Aucun fichier `.env` ni aucune base démarrée n'est nécessaire pour les tests.

## 4. Commandes

Depuis `backend/` (Git Bash sous Windows) :

```bash
./mvnw clean verify                                   # tous les tests + rapport JaCoCo
./mvnw test -Dtest='*IntegrationTest'                 # seulement les tests repository / intégration
./mvnw test -Dtest=InscriptionRepositoryIntegrationTest
./mvnw test -Dtest='!*IntegrationTest,!UniflowApplicationTests'   # seulement les tests unitaires (sans Docker)
```

Rapport de couverture : `backend/target/site/jacoco/index.html`.

## 5. Durée indicative et résultat de référence

Mesure du 2026-11-22 (Windows 11, Docker Desktop 29.2, images déjà en cache), avec le conteneur `uniflow-db` **arrêté** pour prouver que les tests n'en dépendent pas :

```text
$ docker stop uniflow-db
$ ./mvnw -B clean verify
...
Testcontainers version: 2.0.5
Container testcontainers/ryuk:0.14.0 started in PT0.9S
Container postgres:16-alpine started in PT2.2S
Container is started (JDBC URL: jdbc:postgresql://localhost:57774/test?loggerLevel=OFF)
...
[INFO] Tests run: 182, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  35.323 s
```

| Étape | Durée |
|---|---|
| Démarrage du conteneur PostgreSQL | environ 2 à 3 s |
| Premier contexte Spring (`@DataJpaTest`) avec Flyway | environ 10 à 15 s |
| Build complet (182 tests) | environ 35 à 50 s |
| Premier lancement sur une machine neuve | quelques dizaines de secondes de plus (téléchargement des images) |

Couverture JaCoCo après IP-48 :

- packages `etudiant`, `formation` et `inscription` : **100 % des lignes** (controller, service, entity, dto) ;
- package `erreur` : **97,2 % des lignes** ;
- total : **98,9 % des lignes et 91,5 % des branches**.

Les repositories sont des interfaces Spring Data : JaCoCo ne les mesure pas, mais les tests `*RepositoryIntegrationTest` exécutent leurs requêtes sur la vraie base.

## 6. Problèmes fréquents

| Symptôme | Cause | Solution |
|---|---|---|
| `Could not find a valid Docker environment` | Docker Desktop n'est pas lancé. | Lancer Docker Desktop, attendre « Engine running », puis relancer. |
| `client version 1.32 is too old` | Testcontainers trop ancien pour Docker Engine 29 et suivants. | Garder la version du BOM Spring Boot 4.1 (Testcontainers 2.0.5). Ne pas forcer une version 1.x. |
| Premier lancement très long, ou `pull access denied` / timeout | Téléchargement de `postgres:16-alpine` et `testcontainers/ryuk`, ou réseau ou proxy bloquant. | Lancer `docker pull postgres:16-alpine`, puis relancer. Vérifier le proxy de Docker Desktop. |
| `Connection to localhost:5432 refused` dans un test | La classe de test démarre Spring sans `@Import(TestcontainersConfiguration.class)`. | Ajouter l'import. `@DataJpaTest` doit aussi avoir `@AutoConfigureTestDatabase(replace = NONE)`. |
| Un test passe seul mais échoue avec les autres | Il dépend de données d'un autre test, du seed, ou d'un total global. | Créer des données uniques avec `TestData` et ne vérifier que ses propres identifiants. |
| Des conteneurs `postgres:16-alpine` restent après un arrêt brutal (Ctrl+C) | Ryuk les supprime normalement en quelques secondes. | `docker ps --filter label=org.testcontainers=true`, puis les arrêter si besoin. Ne pas toucher à `uniflow-db-1` ni à `uniflow-db`. |

## 7. Nettoyage CI possible (non réalisé dans IP-48)

Avec Testcontainers, le job `backend` de [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) n'a plus besoin de base externe. Les runners `ubuntu-latest` fournissent Docker, et Testcontainers y fonctionne sans configuration.

Éléments **devenus inutiles**, à retirer dans un ticket dédié :

- le bloc `services: postgres:` (image, `env`, `ports: 5432:5432`, `options` du healthcheck) ;
- le bloc `env:` du job : `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` ;
- les variables et secrets GitHub `DB_NAME`, `DB_USERNAME` et `DB_PASSWORD`, s'ils ne servent nulle part ailleurs (à vérifier avant de les supprimer, par exemple pour un futur déploiement).

L'étape `./mvnw -B verify` reste inchangée. Gains attendus : un job plus simple, pas de secret de base nécessaire pour les tests, et un comportement identique en local et en CI.

## 8. Front-end (IP-46)

Les tests du front-end Angular sont des tests **unitaires et de composants**, exécutés par **Vitest** via le builder `@angular/build:unit-test`, dans un DOM simulé (`jsdom`). Ils ne nécessitent ni navigateur, ni API, ni Docker.

### Ce qui est testé

| Zone | Outils | Ce qui est vérifié |
|---|---|---|
| Services (`src/app/services`) | `provideHttpClient()`, `provideHttpClientTesting()`, `HttpTestingController` | URL, verbe HTTP, paramètres et corps de chaque appel à l'API, avec des réponses simulées. Par exemple, `POST /api/inscriptions` avec `{ etudiantId, formationId }` (contrat §5). |
| Intercepteurs | `withInterceptors([...])` + `HttpTestingController` | Indicateur de chargement global (succès, erreur, panne réseau, annulation) et passage direct du mock désactivé |
| Traduction des erreurs | fonctions pures | Code de l'API, puis `detail`, puis statut HTTP (0, 502, 503 et 504 = serveur injoignable) |
| Composants partagés | `TestBed`, `componentRef.setInput()` | Rendu, accessibilité (`role`, `aria-*`) et événements émis |
| Formulaires étudiant et formation | Reactive Forms | Champs obligatoires, longueurs maximales, e-mail, téléphone, date de naissance passée, durée et capacité entières > 0, date de fin ≥ date de début, erreurs serveur par champ, données envoyées |
| Pages | `TestBed` + `HttpTestingController` + `provideRouter` | Chargement, états vides, erreurs 400, 404, 409, 500 et 502, Retry, double clic bloqué, navigation |
| Routes | `RouterTestingHarness` | Chaque URL affiche le bon composant, et les URL inconnues redirigent vers `/students` |

Chaque test configure son propre `TestBed`, vérifie avec `http.verify()` qu'aucune requête n'est restée sans réponse, et ne dépend ni de l'ordre d'exécution ni du mode mock (`useMock`).

### Commandes

Depuis `frontend/` :

```bash
npm test                         # mode « watch » : relance les tests à chaque modification
npm test -- --watch=false        # une seule exécution (commande utilisée par la CI)
npm run test:coverage            # une exécution + rapport de couverture dans frontend/coverage/
```

Rapport HTML : `frontend/coverage/uniflow-frontend/index.html`. Le dossier `coverage/` est ignoré par Git.

Vérification complète avant une Pull Request :

```bash
npm run lint && npm run format:check && npm test -- --watch=false && npm run build
```

### Résultat de référence (2026-11-22)

```text
 Test Files  24 passed (24)
      Tests  165 passed (165)
   Duration  3.58s
```

Couverture (`npm run test:coverage`) :

| Dossier | Instructions | Branches | Fonctions | Lignes |
|---|---|---|---|---|
| `services` | 100,0 % | 97,7 % | 100,0 % | 100,0 % |
| `interceptors` | 32,5 % | 6,7 % | 30,0 % | 33,3 % |
| `components` | 100,0 % | 100,0 % | 100,0 % | 100,0 % |
| `pages` | 85,1 % | 83,1 % | 74,8 % | 89,6 % |
| **Total** | **86,5 %** | **84,0 %** | **77,5 %** | **89,4 %** |

Pour `interceptors`, `loading.interceptor.ts` est couvert à 100 %. Le faible pourcentage vient de `mock-api.interceptor.ts`, le simulateur d'API de développement, dont seul le passage direct est testé : ses réponses simulées ne font pas partie du comportement livré.

### Problèmes fréquents

| Symptôme | Cause | Solution |
|---|---|---|
| `Code coverage requires "@vitest/coverage-v8"` | Dépendances non installées | `npm ci` dans `frontend/` |
| `Expected no open requests, found 1` | Un test déclenche une requête HTTP sans y répondre | Ajouter `http.expectOne(...).flush(...)` pour cette requête |
| `Expected one matching request ..., found none` | L'URL ou les paramètres ne correspondent pas | Utiliser un prédicat `(req) => req.url === '/api/...'` et vérifier `req.params` séparément |
| Le mode watch ne se ferme pas en CI | `npm test` seul reste en mode watch | Toujours passer `-- --watch=false` en CI |
