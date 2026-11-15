# UniFlow — Périmètre et exigences fonctionnelles

> Ticket : **IP-19**. Ce document fait référence pour le contrat d'API (IP-21), les développements des sprints 1 et 2 et les tests.
> Il est aligné sur le schéma de base de données existant (`V1__init.sql`, IP-10 / IP-12).

## 1. Contexte et objectif

UniFlow permet à un **gestionnaire** de gérer les étudiants, les formations et les inscriptions des étudiants aux formations.
Le projet vise une application fonctionnelle **et** une chaîne DevOps complète (Git, CI/CD, Docker, Kubernetes).

| Élément | Choix |
|---|---|
| Acteur unique | Gestionnaire (pas de compte ni de connexion) |
| Interface principale | **API REST** (préfixe `/api`, format JSON) |
| Interface secondaire | Front-end Angular **simple**, qui consomme uniquement l'API |
| Persistance | PostgreSQL, schéma géré par Flyway |

## 2. Modèle métier

```
Etudiant 1 ──── 0..* Inscription 0..* ──── 1 Formation
```

### 2.1 Étudiant (`etudiant`)

| Champ | Type | Obligatoire | Règles |
|---|---|---|---|
| `id_etudiant` | entier | auto | Identifiant généré |
| `nom` | texte (100) | ✅ | Non vide |
| `prenom` | texte (100) | ✅ | Non vide |
| `email` | texte (255) | ✅ | Format e-mail valide, **unique** |
| `telephone` | texte (30) | — | Chiffres, espaces, `+`, `-` uniquement |
| `adresse` | texte (255) | — | — |
| `date_naissance` | date | — | Dans le passé |

### 2.2 Formation (`formation`)

| Champ | Type | Obligatoire | Règles |
|---|---|---|---|
| `id_formation` | entier | auto | Identifiant généré |
| `intitule` | texte (150) | ✅ | Non vide, **unique** |
| `duree` | entier | ✅ | Strictement positif, exprimé en **heures** |
| `capacite_max` | entier | ✅ | Strictement positif |
| `statut` | énumération | auto | `OUVERTE` (défaut), `FERMEE`, `ARCHIVEE` |

### 2.3 Inscription (`inscription`)

| Champ | Type | Obligatoire | Règles |
|---|---|---|---|
| `id_inscription` | entier | auto | Identifiant généré |
| `id_etudiant` | entier | ✅ | Étudiant existant |
| `id_formation` | entier | ✅ | Formation existante |
| `date_inscription` | date | auto | Date du jour par défaut |
| `statut` | énumération | auto | `EN_ATTENTE` (défaut), `VALIDEE`, `ANNULEE` |

Un étudiant ne peut être inscrit **qu'une seule fois** à une même formation.

## 3. Exigences fonctionnelles

Chaque exigence est reliée à son ticket Jira pour la traçabilité.

### 3.1 Gestion des étudiants

| ID | Exigence | Ticket |
|---|---|---|
| RF-01 | Créer un étudiant avec les champs du §2.1 | IP-23 |
| RF-02 | Consulter la liste des étudiants | IP-24 |
| RF-03 | Consulter le détail d'un étudiant par son identifiant | IP-24 |
| RF-04 | Modifier un étudiant (mêmes règles qu'à la création) | IP-25 |
| RF-05 | Supprimer un étudiant | IP-37 |

### 3.2 Gestion des formations

| ID | Exigence | Ticket |
|---|---|---|
| RF-06 | Créer une formation avec les champs du §2.2 | IP-26 |
| RF-07 | Consulter la liste des formations | IP-27 |
| RF-08 | Consulter le détail d'une formation, avec son nombre de places restantes | IP-27 |
| RF-09 | Modifier une formation, y compris son statut | IP-28 |
| RF-10 | Supprimer une formation | IP-38 |

### 3.3 Gestion des inscriptions

| ID | Exigence | Ticket |
|---|---|---|
| RF-11 | Inscrire un étudiant existant à une formation existante | IP-33 |
| RF-12 | Consulter la liste des inscriptions et le détail d'une inscription | IP-35 |
| RF-13 | Valider une inscription en attente | IP-34 |
| RF-14 | Annuler une inscription | IP-36 |

### 3.4 Recherche

| ID | Exigence | Ticket |
|---|---|---|
| RF-15 | Rechercher des étudiants par nom ou prénom : correspondance **partielle** et **insensible à la casse**. Une recherche vide renvoie tous les étudiants. Aucun résultat renvoie une liste vide, pas une erreur. | IP-39 |
| RF-16 | Lister les étudiants inscrits à une formation donnée (inscriptions `EN_ATTENTE` et `VALIDEE`). Une formation inconnue renvoie une erreur 404. | IP-40 |

## 4. Règles métier

### 4.1 Unicité
- **RG-01** — L'e-mail d'un étudiant est unique. Il est enregistré en minuscules pour éviter les doublons du type `Ali@x.ma` / `ali@x.ma`.
- **RG-02** — L'intitulé d'une formation est unique.
- **RG-03** — Un étudiant ne peut pas être inscrit deux fois à la même formation, quel que soit le statut de l'inscription existante. Pour revenir dans une formation après une annulation, l'inscription annulée est remise `EN_ATTENTE`.

### 4.2 Capacité
- **RG-04** — Les inscriptions **actives** sont celles au statut `EN_ATTENTE` ou `VALIDEE`. Les inscriptions `ANNULEE` ne comptent pas.
- **RG-05** — Places restantes = `capacite_max` − nombre d'inscriptions actives.
- **RG-06** — Une inscription est refusée si la formation n'a plus de place.
- **RG-07** — La capacité d'une formation ne peut pas être réduite en dessous de son nombre d'inscriptions actives.
- **RG-08** — Annuler une inscription libère une place.

### 4.3 Statut d'une formation

| Statut | Nouvelles inscriptions | Modification |
|---|---|---|
| `OUVERTE` | ✅ autorisées | ✅ |
| `FERMEE` | ❌ refusées | ✅ |
| `ARCHIVEE` | ❌ refusées | ✅ (pour la rouvrir) |

- **RG-09** — Seule une formation `OUVERTE` accepte de nouvelles inscriptions.

### 4.4 Cycle de vie d'une inscription

```
EN_ATTENTE ──valider──▶ VALIDEE
    │                      │
    └──annuler──▶ ANNULEE ◀┘
```

- **RG-10** — Transitions autorisées : `EN_ATTENTE → VALIDEE`, `EN_ATTENTE → ANNULEE`, `VALIDEE → ANNULEE`.
- **RG-11** — Toute autre transition est refusée. Exemple : `ANNULEE → VALIDEE`, ou valider une inscription déjà validée.

### 4.5 Suppression
- **RG-12** — Supprimer un étudiant ou une formation supprime aussi ses inscriptions (`ON DELETE CASCADE` dans le schéma).
  L'interface doit afficher une **confirmation** indiquant le nombre d'inscriptions supprimées.

## 5. Erreurs et réponses HTTP

| Situation | Code HTTP | Exemple |
|---|---|---|
| Création réussie | `201 Created` | Nouvel étudiant |
| Lecture ou modification réussie | `200 OK` | — |
| Suppression réussie | `204 No Content` | — |
| Données invalides | `400 Bad Request` | E-mail mal formé, durée négative. Réponse avec le **détail par champ**. |
| Ressource inconnue | `404 Not Found` | Étudiant ou formation inexistant |
| Conflit métier | `409 Conflict` | E-mail déjà utilisé, inscription en double, formation complète ou non ouverte, transition de statut interdite, capacité réduite sous les inscriptions actives |
| Erreur inattendue | `500 Internal Server Error` | Message générique, **sans trace technique ni secret** |

Le format exact des réponses d'erreur est défini dans le contrat d'API (IP-21) et implémenté dans IP-22.

## 6. Exigences non fonctionnelles

| ID | Exigence |
|---|---|
| RNF-01 | Java 21, Spring Boot, Maven, PostgreSQL, Flyway |
| RNF-02 | Toute la configuration sensible passe par des variables d'environnement, sans secret dans Git |
| RNF-03 | L'application démarre avec `docker compose up --build`, puis sur Kubernetes (Minikube) |
| RNF-04 | Point de santé `/actuator/health`, utilisé par Docker et les probes Kubernetes |
| RNF-05 | Tests JUnit 5 et couverture d'au moins 80 % sur le nouveau code (Quality Gate SonarQube) |
| RNF-06 | Chaque modification passe par une Pull Request relue et une CI verte |

## 7. Hors périmètre

Les éléments suivants ne sont **pas** réalisés dans ce projet :

- authentification, comptes utilisateurs et rôles ;
- espace étudiant (les étudiants n'utilisent pas l'application) ;
- paiement des formations ;
- envoi d'e-mails ou de notifications ;
- planning détaillé des sessions (salles, horaires, formateurs) ;
- notes, examens et certificats ;
- import ou export de fichiers (CSV, Excel, PDF) ;
- interface avancée : l'interface reste simple, comme le demande le cahier des charges.

## 8. Points à valider par l'équipe

| Point | Proposition | Décision |
|---|---|---|
| Dates de formation : le backlog (IP-26) prévoit des dates et la règle « date de fin ≥ date de début », mais le schéma V1 n'a pas de colonnes de dates | Ajouter `date_debut` et `date_fin` dans une migration `V2`, **ou** retirer ce critère d'IP-26 | _à décider_ |
| Description de formation : prévue dans IP-26, absente du schéma | Ajouter une colonne `description` (texte, facultative) dans la même migration `V2` | _à décider_ |
| Unité de `duree` | Heures | _à confirmer_ |
| Suppression avec inscriptions | Cascade (schéma actuel) + confirmation dans l'interface | _à confirmer_ |