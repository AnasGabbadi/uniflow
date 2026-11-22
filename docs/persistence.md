# Persistance des données PostgreSQL (IP-49)

Ce document explique comment les données de la base UniFlow sont conservées entre deux redémarrages des conteneurs, et comment le vérifier.

## 1. Le volume nommé `db-data`

Un conteneur est jetable : tout ce qui est écrit dans son système de fichiers disparaît quand il est supprimé (`docker compose down`). PostgreSQL range ses données dans `/var/lib/postgresql/data`. Ce dossier est donc monté sur un **volume nommé** : un espace de stockage géré par Docker, indépendant du cycle de vie des conteneurs.

Dans [`compose.yaml`](../compose.yaml) :

```yaml
name: uniflow

services:
  db:
    image: postgres:16-alpine
    volumes:
      - db-data:/var/lib/postgresql/data   # montage du volume dans le conteneur

volumes:
  db-data:                                 # déclaration du volume nommé
```

Docker préfixe le nom du volume par le nom du projet. Le volume réel s'appelle donc **`uniflow_db-data`** :

```bash
docker volume ls --filter label=com.docker.compose.project=uniflow
# DRIVER    VOLUME NAME
# local     uniflow_db-data
```

> **Conséquence importante** : PostgreSQL n'initialise la base (utilisateur, mot de passe, nom de la base) **qu'au premier démarrage, quand le volume est vide**. Ensuite, modifier `POSTGRES_PASSWORD` dans `.env` ne change plus rien à la base existante (voir « Problèmes fréquents »).

## 2. `docker compose down` ou `docker compose down -v`

| Commande | Conteneurs | Réseau | Volume `uniflow_db-data` | Données |
|---|---|---|---|---|
| `docker compose stop` | arrêtés (conservés) | conservé | conservé | **conservées** |
| `docker compose down` | supprimés | supprimé | **conservé** | **conservées** |
| `docker compose down -v` | supprimés | supprimé | **supprimé** | **perdues** |

`down -v` ne sert qu'à repartir d'une base vierge : Flyway rejoue alors toutes les migrations au démarrage suivant.

## 3. Remarque sur les données du seed

La migration `V2__seed_dev.sql` insère des données de démonstration (4 formations, 5 étudiants, 6 inscriptions, un compte admin). Elle est appliquée par Flyway **dans tous les environnements**, chaque fois que la base est vide.

Ces données **ne prouvent donc pas** la persistance : elles réapparaissent même après un `down -v`, puisque Flyway les recrée. Le test doit porter sur une donnée **créée à la main après le démarrage**. C'est le rôle de la formation de test horodatée utilisée ci-dessous.

## 4. Le conteneur `uniflow-db` (hors Compose)

`docker ps -a` peut aussi afficher un conteneur nommé **`uniflow-db`** (sans suffixe `-1`). Ce conteneur ne fait **pas** partie de la stack Compose : c'est la base PostgreSQL de test utilisée par `./mvnw verify` sur `localhost:5432`.

Il ne porte pas le label `com.docker.compose.project=uniflow` et utilise son propre volume. Les vérifications filtrent donc par **label** et non par nom : un filtre `name=uniflow` trouverait ce conteneur et ferait croire à tort qu'un conteneur de la stack a survécu au `down`. Il ne faut **pas** le supprimer.

## 5. Vérification automatique : `scripts/verify-persistence.sh`

Depuis la racine du dépôt (Git Bash, Linux ou macOS), avec un `.env` rempli :

```bash
./scripts/verify-persistence.sh
echo $?   # 0 = PERSISTANCE OK, 1 = PERSISTANCE KO
```

Variables facultatives : `API_URL` (défaut `http://localhost:<APP_PORT>`) et `WAIT_TIMEOUT` (défaut 180 s).

Le script :

1. lance `docker compose up -d --build` et attend que `/actuator/health` réponde `UP` (interrogation toutes les 2 s, délai maximal `WAIT_TIMEOUT`) ;
2. crée par `POST /api/formations` une formation nommée `Test persistance <date heure>` ;
3. lance `docker compose down` (**sans `-v`**), vérifie qu'aucun conteneur portant le label `com.docker.compose.project=uniflow` ne subsiste et que le volume existe toujours ;
4. relance la stack et attend de nouveau l'API ;
5. vérifie la formation via l'API (`GET /api/formations/{id}`) **et** directement en base (`docker compose exec -T db psql`) ;
6. supprime la formation de test (`DELETE /api/formations/{id}`, réponse 204) et affiche `PERSISTANCE OK` ou `PERSISTANCE KO`.

Le script ne lit dans `.env` que `APP_PORT`. `psql` utilise les identifiants déjà présents dans le conteneur : aucun mot de passe n'est affiché.

## 6. Procédure manuelle pas à pas

Commandes pour Git Bash. Sous PowerShell, remplacer `\` par `` ` `` pour les retours à la ligne.

```bash
# 1. Démarrer la stack et vérifier l'API
docker compose up -d --build
docker compose ps                                   # db : healthy
curl http://localhost:8080/actuator/health          # {"status":"UP",...}

# 2. Repérer le volume
docker volume ls --filter label=com.docker.compose.project=uniflow

# 3. Créer une donnée de test (noter l'"id" renvoyé)
curl -i -X POST http://localhost:8080/api/formations \
  -H "Content-Type: application/json" \
  -d '{"intitule":"Test persistance manuel","duree":10,"capaciteMax":20,"dateDebut":"2026-12-01","dateFin":"2026-12-05"}'

# 4. Contrôler en base
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id_formation, intitule FROM formation"'

# 5. Arrêter et supprimer les conteneurs (SANS -v)
docker compose down
docker ps -a --filter label=com.docker.compose.project=uniflow   # liste vide attendue
docker volume ls --filter label=com.docker.compose.project=uniflow  # le volume est toujours là

# 6. Redémarrer, attendre l'API puis revérifier
docker compose up -d
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/formations/<id>
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id_formation, intitule FROM formation"'

# 7. Nettoyer
curl -i -X DELETE http://localhost:8080/api/formations/<id>      # HTTP 204
```

## 7. Résultats réels (exécution du 2026-11-22)

### Test principal : `./scripts/verify-persistence.sh`

Sortie du script, sans les journaux de build Docker :

```text
===== Vérification de la persistance PostgreSQL (IP-49) =====
[10:20:36] Étape 1/6 : docker compose up -d --build
[10:22:04] Attente de l'API sur http://localhost:8080/actuator/health (max 180s)...
[10:22:13] API prête en 9s.
[10:22:14] Volume nommé utilisé : uniflow_db-data
[10:22:14] Étape 2/6 : création de la formation « Test persistance 2026-11-22 10:22:14 »
[10:22:14] Formation créée : HTTP 201, id=6
[10:22:15] Avant arrêt, en base : 6 | Test persistance 2026-11-22 10:22:14
[10:22:15] Étape 3/6 : docker compose down (sans -v)
 Container uniflow-frontend-1 Removed
 Container uniflow-app-1 Removed
 Container uniflow-db-1 Removed
 Network uniflow_default Removed
[10:22:17] Aucun conteneur du projet 'uniflow' ne subsiste.
[10:22:18] Le volume uniflow_db-data est toujours présent.
[10:22:18] Étape 4/6 : docker compose up -d
[10:22:26] Attente de l'API sur http://localhost:8080/actuator/health (max 180s)...
[10:22:34] API prête en 8s.
[10:22:34] Étape 5/6 : vérification après redémarrage
[10:22:35] API  : OK  -> GET /api/formations/6 = 200, intitulé identique
[10:22:35] psql : OK  -> ligne trouvée : 6 | Test persistance 2026-11-22 10:22:14
[10:22:35] Étape 6/6 : nettoyage (route DELETE /api/formations/{id} disponible)
[10:22:35] Formation de test 6 supprimée (HTTP 204).

===== PERSISTANCE OK =====
Code de sortie : 0
```

### Contre-épreuve : `docker compose down -v`

But : montrer que c'est bien le volume qui conserve les données. On le supprime volontairement, et la donnée doit disparaître.

```bash
# 1. Formation témoin (id 7)
curl -X POST http://localhost:8080/api/formations -H "Content-Type: application/json" \
  -d '{"intitule":"Contre-epreuve 2026-11-22 10:24:41","duree":10,"capaciteMax":20,"dateDebut":"2026-12-01","dateFin":"2026-12-05"}'
# {"id":7,"intitule":"Contre-epreuve 2026-11-22 10:24:41",...,"statut":"OUVERTE","placesRestantes":20}   HTTP 201
```

```text
 id_formation |              intitule
--------------+------------------------------------
            1 | Génie Logiciel
            2 | Data Science
            3 | Cybersécurité
            4 | Réseaux et Télécoms
            5 | Génie IT
            7 | Contre-epreuve 2026-11-22 10:24:41
```

```text
$ docker compose down -v
 Container uniflow-frontend-1 Removed
 Container uniflow-app-1 Removed
 Container uniflow-db-1 Removed
 Volume uniflow_db-data Removed          <- le volume est supprimé
 Network uniflow_default Removed

$ docker volume ls --filter label=com.docker.compose.project=uniflow
DRIVER    VOLUME NAME                    <- plus aucun volume

$ docker compose up -d
 Volume uniflow_db-data Creating         <- nouveau volume vide
 Volume uniflow_db-data Created
 ...
```

Après redémarrage :

```text
-- formation témoin
 formation_temoin
------------------
                0                        <- la donnée a disparu

-- contenu de la table formation : uniquement le seed V2, recréé par Flyway
 id_formation |      intitule       |  statut
--------------+---------------------+----------
            1 | Génie Logiciel      | OUVERTE
            2 | Data Science        | OUVERTE
            3 | Cybersécurité       | ARCHIVEE
            4 | Réseaux et Télécoms | FERMEE

 etudiants | inscriptions
-----------+--------------
         5 |            6

-- flyway_schema_history : migrations rejouées sur la base vierge
 version |              description               | success
---------+----------------------------------------+---------
 1       | init                                   | t
 2       | seed dev                               | t
 3       | ajouter description et dates formation | t
```

**Conclusion** :
- Avec `down`, la formation créée à la main survit au redémarrage.
- Avec `down -v`, elle disparaît et seules les données du seed reviennent : la formation 1 repasse à `OUVERTE`, et la formation 5 « Génie IT », créée à la main, est perdue.

La persistance repose donc bien sur le volume nommé `uniflow_db-data`.

## 8. Problèmes fréquents

| Symptôme | Cause | Solution |
|---|---|---|
| `password authentication failed for user "uniflow"` dans `docker compose logs app` | `POSTGRES_PASSWORD` / `DB_PASSWORD` a été changé dans `.env` **après** la création du volume : la base garde l'ancien mot de passe. | Remettre l'ancien mot de passe dans `.env`, ou repartir d'une base vierge avec `docker compose down -v` (**toutes les données sont perdues**). |
| `Bind for 0.0.0.0:8080 failed: port is already allocated` (ou 4200) | Un autre programme ou conteneur utilise le port. | Arrêter ce programme, ou changer `APP_PORT` / `FRONTEND_PORT` dans `.env`. Le script lit `APP_PORT` automatiquement. |
| `curl: (7) Failed to connect` ou `(52) Empty reply` juste après `up` | L'API n'est pas encore prête : Flyway et Spring Boot démarrent encore. | Attendre que `/actuator/health` réponde `UP` (le script le fait tout seul). Si l'attente dépasse `WAIT_TIMEOUT` : `docker compose logs app`. |
| `the input device is not a TTY` sous Git Bash | `docker compose exec` sans `-T` dans un terminal mintty. | Ajouter `-T`, ou préfixer par `winpty`. |
| Le script affiche `PERSISTANCE KO` après un `down` | Le volume a été supprimé (`down -v`, `docker volume rm`, `docker system prune --volumes`), ou `compose.yaml` ne monte plus `db-data`. | Vérifier `docker volume ls` et la section `volumes:` de `compose.yaml`. |
| Un conteneur `uniflow-db` apparaît dans `docker ps -a` | Base de test de `./mvnw verify`, hors Compose (voir section 4). | Normal. Ne pas le supprimer. |
