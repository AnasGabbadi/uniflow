# Front-end UniFlow

Interface Angular qui consomme l'API REST UniFlow.

## Prérequis
- Node.js 20.19 ou plus récent
- Angular CLI : `npm install -g @angular/cli`

## Commandes
| Commande | Rôle |
|---|---|
| `npm install` | Installer les dépendances |
| `npm start` | Lancer en développement sur http://localhost:4200 |
| `npm run build` | Construire la version de production dans `dist/` |
| `npm run lint` | Vérifier le code avec ESLint |
| `npm run format` | Formater le code avec Prettier |
| `npm run format:check` | Vérifier le formatage sans modifier les fichiers |
| `npm test` | Lancer les tests en mode « watch » (relance à chaque modification) |
| `npm test -- --watch=false` | Lancer les tests une seule fois (commande de la CI) |
| `npm run test:coverage` | Lancer les tests une fois et produire le rapport de couverture |

## Tests
Les tests (`*.spec.ts`) utilisent **Vitest** via `ng test`, dans un DOM simulé : ni navigateur, ni API, ni Docker ne sont nécessaires.
Les appels HTTP sont simulés avec `HttpTestingController`.

```bash
npm test -- --watch=false   # une exécution : 24 fichiers, 165 tests
npm run test:coverage       # idem + rapport dans coverage/uniflow-frontend/index.html
```

Le dossier `coverage/` est ignoré par Git. Vérification complète avant une Pull Request :

```bash
npm run lint && npm run format:check && npm test -- --watch=false && npm run build
```

Stratégie, règles d'écriture et couverture de référence : [docs/tests.md](../docs/tests.md), section « Front-end ».

## Appels à l'API
Le front appelle toujours l'API via l'adresse relative `/api` (`src/environments/`).
- En développement, `proxy.conf.json` redirige `/api` vers `http://localhost:8080`.
- Dans Docker et Kubernetes, Nginx redirige `/api` vers le service back-end.

## Structure
- `src/app/pages` : un dossier par écran
- `src/app/components` : composants réutilisables
- `src/app/services` : appels à l'API
- `src/app/models` : types Student, Training, Enrollment
