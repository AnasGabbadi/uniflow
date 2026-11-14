# ---------- Étape 1 : build Angular ----------
FROM node:22-alpine AS build
WORKDIR /app

# Dépendances d'abord, pour profiter du cache Docker
COPY package.json package-lock.json ./
RUN npm ci

# Code source puis build de production
COPY . .
RUN npm run build -- --configuration production

# ---------- Étape 2 : exécution avec Nginx ----------
FROM nginx:1.27-alpine

# Modèle de configuration : ${API_UPSTREAM} est remplacé au démarrage du conteneur
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

# Fichiers statiques produits par Angular
COPY --from=build /app/dist/uniflow-frontend/browser /usr/share/nginx/html

# Adresse de l'API par défaut (surchargée par Docker Compose ou Kubernetes)
ENV API_UPSTREAM=http://app:8080

EXPOSE 80