# Definition of Done

Un ticket Jira ne passe à **Terminé** que si **toutes** les conditions suivantes sont remplies.
Ces règles ont été validées par toute l'équipe (IP-2).

## Code
- [ ] Les critères d'acceptation du ticket sont tous satisfaits.
- [ ] Le code est sur une branche `feature/IP-<n>-<description>` créée depuis `develop`.
- [ ] Aucun mot de passe, token ou fichier `.env` n'est versionné.

## Tests et qualité
- [ ] Les tests existants passent, et les nouveaux comportements sont testés.
- [ ] La CI GitHub Actions est verte.
- [ ] La Quality Gate SonarQube est validée.
- [ ] Le lint du front-end passe (`npm run lint`) si le front est modifié.

## Revue
- [ ] Une Pull Request vers `develop` a été ouverte avec la clé Jira dans le titre.
- [ ] Au moins un autre membre a relu et approuvé la PR.
- [ ] La PR est fusionnée et la branche supprimée.

## Documentation et preuves
- [ ] Le README ou la documentation est mis à jour si le fonctionnement change.
- [ ] Les preuves sont jointes au ticket Jira : capture, résultat de test, lien du pipeline ou de la PR.