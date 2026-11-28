# Déploiement UniFlow sur Kubernetes

Ce dossier contient les manifestes Kubernetes permettant de déployer l'API UniFlow avec PostgreSQL.

## Prérequis

- Docker Desktop avec Kubernetes activé.
- Docker et kubectl installés.
- L'image Docker `uniflow:latest` disponible localement.

## 1. Vérifier Kubernetes

```powershell
kubectl config current-context
kubectl get nodes
```

Le contexte utilisé est `docker-desktop`.

## 2. Créer le namespace et la configuration

```powershell
kubectl apply -f .\kubernetes\namespace.yaml
kubectl apply -f .\kubernetes\configmap.yaml
```

## 3. Créer le Secret

Le Secret `uniflow-secret` contient les variables sensibles :
- `POSTGRES_PASSWORD`
- `DB_PASSWORD`

Les mots de passe ne doivent jamais être enregistrés en clair dans Git.

## 4. Déployer PostgreSQL

```powershell
kubectl apply -f .\kubernetes\postgres-statefulset.yaml
kubectl apply -f .\kubernetes\postgres-service.yaml
```

Vérifier :

```powershell
kubectl get pods -n uniflow
kubectl get pvc -n uniflow
kubectl get services -n uniflow
```

## 5. Déployer l'API

```powershell
kubectl apply -f .\kubernetes\deployment.yaml
kubectl apply -f .\kubernetes\service.yaml
```

Vérifier :

```powershell
kubectl get deployments -n uniflow
kubectl get pods -n uniflow
kubectl logs deployment/uniflow-api -n uniflow
```

L'API se connecte à PostgreSQL via :

`jdbc:postgresql://postgres:5432/uniflow`

## 6. Accéder à l'API depuis Windows

Le Service `uniflow-api` est de type `ClusterIP`.

```powershell
kubectl port-forward -n uniflow service/uniflow-api 8081:8080
```

Garder ce terminal ouvert.

URL de santé : http://localhost:8081/actuator/health

Documentation Swagger : http://localhost:8081/swagger-ui.html

Test PowerShell :

```powershell
Invoke-WebRequest http://localhost:8081/actuator/health -UseBasicParsing
```

Une réponse HTTP 200 avec le statut `UP` confirme que l'API répond correctement.

Pour arrêter le port-forward, utiliser `Ctrl + C`.

## 7. Nettoyage

Attention : cette commande supprime toutes les ressources du namespace, y compris le stockage persistant.

```powershell
kubectl delete namespace uniflow
```