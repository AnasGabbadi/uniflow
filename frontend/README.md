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

## Appels à l'API
Le front appelle toujours l'API via l'adresse relative `/api` (`src/environments/`).
- En développement, `proxy.conf.json` redirige `/api` vers `http://localhost:8080`.
- Dans Docker et Kubernetes, Nginx redirige `/api` vers le service back-end.

## Structure
- `src/app/pages` : un dossier par écran
- `src/app/components` : composants réutilisables
- `src/app/services` : appels à l'API
- `src/app/models` : types Student, Training, Enrollment
