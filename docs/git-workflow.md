# Workflow Git de l'équipe UniFlow

Ce document décrit les règles de branches, de commits et de Pull Requests. Il s'inspire de Git Flow (TP 2) et s'applique à tous les membres.

## 1. Branches

| Branche | Rôle | Créée depuis | Fusionnée vers |
|---|---|---|---|
| `main` | Versions stables, livrées et taguées | — | — |
| `develop` | Intégration des fonctionnalités terminées | `main` | `main` (release) |
| `feature/IP-<n>-<description>` | Un ticket Jira | `develop` | `develop` |
| `hotfix/IP-<n>-<description>` | Correction urgente d'une version livrée | `main` | `main`, puis `develop` |

Règles de nommage :
- toujours la clé Jira `IP-<n>` juste après le préfixe ;
- description courte, en minuscules, mots séparés par des tirets ;
- exemples : `feature/IP-23-ajout-etudiant`, `hotfix/IP-70-email-duplique`.

## 2. Protection des branches

`main` et `develop` sont protégées par un ruleset GitHub :
- aucun push direct : tout passe par une Pull Request ;
- au moins **1 approbation** d'un autre membre avant la fusion ;
- force push et suppression interdits ;
- à partir d'IP-6 : la CI doit être verte avant la fusion ;
- à partir d'IP-7 : la Quality Gate SonarQube doit être validée.

## 3. Cycle d'une fonctionnalité

```bash
# 1. Partir d'un develop à jour
git switch develop
git pull

# 2. Créer la branche du ticket
git switch -c feature/IP-23-ajout-etudiant

# 3. Travailler et committer régulièrement
git add .
git commit -m "IP-23 feat: ajouter la création d'un étudiant"

# 4. Récupérer les derniers changements de develop avant la PR
git fetch origin
git merge origin/develop        # résoudre les conflits éventuels ici

# 5. Publier la branche
git push -u origin feature/IP-23-ajout-etudiant
```

6. Ouvrir une Pull Request vers `develop` en remplissant le modèle.
7. Passer le ticket Jira en **En revue**.
8. Un autre membre relit, commente puis approuve.
9. Fusionner, supprimer la branche, puis passer le ticket en **Terminé** avec les preuves.

## 4. Messages de commit

Format : `IP-<n> <type>: <message>`

| Type | Usage |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `test` | Ajout ou modification de tests |
| `docs` | Documentation |
| `ci` | Pipeline CI/CD |
| `chore` | Configuration, dépendances, structure |
| `refactor` | Amélioration du code sans changement de comportement |

Un commit = une modification cohérente. Éviter les messages vagues comme `update` ou `fix`.

## 5. Revue de code

Le relecteur vérifie :
- que le code répond aux critères d'acceptation du ticket ;
- que les tests existent et passent ;
- qu'aucun secret, mot de passe ou fichier `.env` n'est versionné ;
- que le code est lisible et cohérent avec le reste du projet.

Les revues se font de préférence **entre binômes différents** : un développeur front relit une PR back et inversement. Chacun comprend ainsi l'ensemble du projet pour la démonstration individuelle.

## 6. Releases et versions

À la fin de chaque sprint, si `develop` est stable :
1. Ouvrir une Pull Request `develop` → `main` intitulée `release: vX.Y.0`.
2. Après fusion, créer le tag :
   ```bash
   git switch main
   git pull
   git tag -a v0.1.0 -m "Sprint 0 : environnement prêt"
   git push origin v0.1.0
   ```
3. Créer la Release GitHub à partir du tag.

Versionnement sémantique `MAJEUR.MINEUR.CORRECTIF` :

| Sprint | Version |
|---|---|
| Sprint 0 | `v0.1.0` |
| Sprint 1 | `v0.2.0` |
| Sprint 2 | `v0.3.0` |
| Sprint 3 (livraison finale) | `v1.0.0` |

Une correction urgente sur une version livrée incrémente le dernier chiffre, par exemple `v0.2.1`.

## 7. Correction urgente (hotfix)

```bash
git switch main
git pull
git switch -c hotfix/IP-70-email-duplique
# corriger, committer, pousser
```
Ouvrir une PR vers `main`, puis, après fusion et tag, une seconde PR `main` → `develop` pour ne pas perdre la correction.

## 8. À ne jamais faire

- Pousser directement sur `main` ou `develop`.
- Utiliser `git push --force` sur une branche partagée.
- Faire un `rebase` d'une branche déjà publiée et utilisée par un autre membre.
- Versionner `.env`, des mots de passe ou des tokens.
