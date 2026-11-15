# UniFlow — Contrat d'API REST

> Ticket : **IP-21**. Contrat commun au binôme back-end (implémentation) et au binôme front-end (consommation).
> Il s'appuie sur [requirements.md](requirements.md) (IP-19) et sur le schéma `V1__init.sql`.
> Toute modification du contrat passe par une Pull Request relue par **un membre de chaque binôme**.

## 1. Conventions générales

| Élément | Règle |
|---|---|
| Préfixe | Toutes les routes commencent par **`/api`** |
| Format | JSON en UTF-8 (`Content-Type: application/json`) |
| Noms des champs JSON | camelCase français : `dateNaissance`, `capaciteMax` |
| Identifiants | Champ `id` (entier) dans toutes les réponses |
| Dates | Format ISO `AAAA-MM-JJ`, par exemple `2026-11-15` |
| Énumérations | En majuscules, exactement comme en base : `OUVERTE`, `EN_ATTENTE`… |
| Listes | Tableau JSON, sans pagination. Liste vide `[]` s'il n'y a aucun résultat |
| Champs facultatifs | Valeur `null` dans les réponses s'ils ne sont pas renseignés |
| CORS | **Aucun réglage nécessaire** : le front appelle `/api` sur sa propre origine. Le proxy Angular (développement) puis Nginx (Docker / Kubernetes) relaient vers l'API |

## 2. Récapitulatif des routes

| Méthode | Route | Rôle | Succès | Exigence |
|---|---|---|---|---|
| `GET` | `/api/etudiants` | Lister les étudiants (recherche facultative) | 200 | RF-02, RF-15 |
| `GET` | `/api/etudiants/{id}` | Détail d'un étudiant | 200 | RF-03 |
| `POST` | `/api/etudiants` | Créer un étudiant | 201 | RF-01 |
| `PUT` | `/api/etudiants/{id}` | Modifier un étudiant | 200 | RF-04 |
| `DELETE` | `/api/etudiants/{id}` | Supprimer un étudiant (et ses inscriptions) | 204 | RF-05 |
| `GET` | `/api/formations` | Lister les formations | 200 | RF-07 |
| `GET` | `/api/formations/{id}` | Détail d'une formation | 200 | RF-08 |
| `POST` | `/api/formations` | Créer une formation | 201 | RF-06 |
| `PUT` | `/api/formations/{id}` | Modifier une formation | 200 | RF-09 |
| `DELETE` | `/api/formations/{id}` | Supprimer une formation (et ses inscriptions) | 204 | RF-10 |
| `GET` | `/api/formations/{id}/etudiants` | Étudiants inscrits à une formation | 200 | RF-16 |
| `GET` | `/api/inscriptions` | Lister les inscriptions (filtres facultatifs) | 200 | RF-12 |
| `GET` | `/api/inscriptions/{id}` | Détail d'une inscription | 200 | RF-12 |
| `POST` | `/api/inscriptions` | Inscrire un étudiant à une formation | 201 | RF-11 |
| `PATCH` | `/api/inscriptions/{id}/statut` | Valider ou annuler une inscription | 200 | RF-13, RF-14 |

## 3. Étudiants

### Objet `Etudiant` (réponse)

```json
{
  "id": 1,
  "nom": "El Amrani",
  "prenom": "Sara",
  "email": "sara.elamrani@example.ma",
  "telephone": "+212 6 12 34 56 78",
  "adresse": "12 rue des Écoles, Casablanca",
  "dateNaissance": "2003-04-21"
}
```

### Corps de requête (`POST` et `PUT`)

Mêmes champs, **sans `id`**.

| Champ | Obligatoire | Validation |
|---|---|---|
| `nom` | ✅ | Non vide, 100 caractères maximum |
| `prenom` | ✅ | Non vide, 100 caractères maximum |
| `email` | ✅ | Format e-mail, 255 caractères maximum, unique. Enregistré en minuscules |
| `telephone` | — | 30 caractères maximum, chiffres, espaces, `+` et `-` uniquement |
| `adresse` | — | 255 caractères maximum |
| `dateNaissance` | — | Date dans le passé |

### Recherche

`GET /api/etudiants?recherche=ali`

- Correspondance **partielle** et **insensible à la casse** sur le nom **ou** le prénom.
- Paramètre absent ou vide : tous les étudiants.
- Tri par `nom` puis `prenom`.

### Erreurs propres aux étudiants

| Cas | Code | `code` |
|---|---|---|
| E-mail déjà utilisé par un autre étudiant | 409 | `EMAIL_DEJA_UTILISE` |

## 4. Formations

### Objet `Formation` (réponse)

```json
{
  "id": 3,
  "intitule": "DevOps avec Docker et Kubernetes",
  "duree": 40,
  "capaciteMax": 25,
  "statut": "OUVERTE",
  "placesRestantes": 7
}
```

`placesRestantes` est calculé par l'API (`capaciteMax` − inscriptions `EN_ATTENTE` et `VALIDEE`). Il n'est **jamais** envoyé par le front.

### Corps de requête (`POST` et `PUT`)

```json
{
  "intitule": "DevOps avec Docker et Kubernetes",
  "duree": 40,
  "capaciteMax": 25,
  "statut": "OUVERTE"
}
```

| Champ | Obligatoire | Validation |
|---|---|---|
| `intitule` | ✅ | Non vide, 150 caractères maximum, unique |
| `duree` | ✅ | Entier strictement positif, en **heures** |
| `capaciteMax` | ✅ | Entier strictement positif, jamais inférieur aux inscriptions actives |
| `statut` | — | `OUVERTE` (défaut), `FERMEE`, `ARCHIVEE` |

### Étudiants d'une formation

`GET /api/formations/{id}/etudiants` renvoie un tableau d'objets `Etudiant` dont l'inscription est `EN_ATTENTE` ou `VALIDEE`.

### Erreurs propres aux formations

| Cas | Code | `code` |
|---|---|---|
| Intitulé déjà utilisé | 409 | `INTITULE_DEJA_UTILISE` |
| Capacité inférieure aux inscriptions actives | 409 | `CAPACITE_INSUFFISANTE` |

## 5. Inscriptions

### Objet `Inscription` (réponse)

Les objets liés sont **résumés** pour éviter les boucles de sérialisation JSON.

```json
{
  "id": 12,
  "dateInscription": "2026-11-15",
  "statut": "EN_ATTENTE",
  "etudiant": {
    "id": 1,
    "nom": "El Amrani",
    "prenom": "Sara",
    "email": "sara.elamrani@example.ma"
  },
  "formation": {
    "id": 3,
    "intitule": "DevOps avec Docker et Kubernetes"
  }
}
```

### Créer une inscription

`POST /api/inscriptions`

```json
{ "etudiantId": 1, "formationId": 3 }
```

- L'inscription est créée au statut `EN_ATTENTE`, à la date du jour.
- Si une inscription **annulée** existe déjà pour ce couple étudiant / formation, elle est **réactivée** (`EN_ATTENTE`, date du jour) au lieu d'être recréée, conformément à RG-03.

### Changer le statut

`PATCH /api/inscriptions/{id}/statut`

```json
{ "statut": "VALIDEE" }
```

| Statut actuel | Vers `VALIDEE` | Vers `ANNULEE` |
|---|---|---|
| `EN_ATTENTE` | ✅ | ✅ |
| `VALIDEE` | ❌ 409 | ✅ |
| `ANNULEE` | ❌ 409 | ❌ 409 |

### Filtres de la liste

`GET /api/inscriptions?etudiantId=1&formationId=3&statut=VALIDEE`. Tous les filtres sont facultatifs et combinables.

### Erreurs propres aux inscriptions

| Cas | Code | `code` |
|---|---|---|
| Étudiant ou formation inexistant | 404 | `RESSOURCE_INTROUVABLE` |
| Étudiant déjà inscrit (inscription active) | 409 | `INSCRIPTION_EN_DOUBLE` |
| Plus de place disponible | 409 | `FORMATION_COMPLETE` |
| Formation `FERMEE` ou `ARCHIVEE` | 409 | `FORMATION_NON_OUVERTE` |
| Transition de statut interdite | 409 | `TRANSITION_INTERDITE` |

## 6. Format des erreurs

Toutes les erreurs utilisent le format standard **Problem Details** (RFC 9457), proposé nativement par Spring Boot (`ProblemDetail`), avec deux champs supplémentaires : `code` et `erreurs`.

### Données invalides (400)

```json
{
  "type": "about:blank",
  "title": "Données invalides",
  "status": 400,
  "detail": "Un ou plusieurs champs sont invalides.",
  "instance": "/api/etudiants",
  "code": "VALIDATION",
  "erreurs": [
    { "champ": "email", "message": "doit être une adresse e-mail valide" },
    { "champ": "nom", "message": "ne doit pas être vide" }
  ]
}
```

### Ressource introuvable (404)

```json
{
  "type": "about:blank",
  "title": "Ressource introuvable",
  "status": 404,
  "detail": "Aucun étudiant avec l'identifiant 42.",
  "instance": "/api/etudiants/42",
  "code": "RESSOURCE_INTROUVABLE"
}
```

### Conflit métier (409)

```json
{
  "type": "about:blank",
  "title": "Conflit",
  "status": 409,
  "detail": "La formation « DevOps avec Docker et Kubernetes » est complète.",
  "instance": "/api/inscriptions",
  "code": "FORMATION_COMPLETE"
}
```

### Erreur inattendue (500)

```json
{
  "type": "about:blank",
  "title": "Erreur interne",
  "status": 500,
  "detail": "Une erreur inattendue est survenue.",
  "instance": "/api/etudiants",
  "code": "ERREUR_INTERNE"
}
```

Aucune trace technique (stack trace), requête SQL ou information sensible n'apparaît dans une réponse.

### Liste complète des codes d'erreur

| `code` | HTTP | Signification |
|---|---|---|
| `VALIDATION` | 400 | Un ou plusieurs champs invalides (détail dans `erreurs`) |
| `REQUETE_INVALIDE` | 400 | JSON mal formé ou paramètre de type incorrect |
| `RESSOURCE_INTROUVABLE` | 404 | Identifiant inconnu |
| `EMAIL_DEJA_UTILISE` | 409 | E-mail d'étudiant en double |
| `INTITULE_DEJA_UTILISE` | 409 | Intitulé de formation en double |
| `CAPACITE_INSUFFISANTE` | 409 | Capacité réduite sous les inscriptions actives |
| `INSCRIPTION_EN_DOUBLE` | 409 | Étudiant déjà inscrit à la formation |
| `FORMATION_COMPLETE` | 409 | Plus de place disponible |
| `FORMATION_NON_OUVERTE` | 409 | Formation fermée ou archivée |
| `TRANSITION_INTERDITE` | 409 | Changement de statut d'inscription non autorisé |
| `ERREUR_INTERNE` | 500 | Erreur inattendue |

Le front affiche un message selon le `code`, plutôt que d'analyser le texte de `detail`.

## 7. Client API côté front-end

Le binôme front utilise les services Angular de `frontend/src/app/services/` et les types de `frontend/src/app/models/`, qui reprennent exactement ce contrat.

En attendant l'API réelle, le front peut fonctionner avec des **données simulées** : passer `useMock: true` dans `src/environments/environment.development.ts`. Le simulateur (`mock-api.interceptor.ts`) répond aux lectures avec des données d'exemple et accepte les écritures sans les enregistrer.