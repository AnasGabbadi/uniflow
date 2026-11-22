#!/usr/bin/env bash
# ============================================================
# IP-49 : vérifie que les données PostgreSQL survivent à un
# « docker compose down » (SANS -v) grâce au volume nommé db-data.
#
# Utilisation (depuis la racine du dépôt, Git Bash / Linux / macOS) :
#   ./scripts/verify-persistence.sh
# Variables facultatives :
#   API_URL      (défaut : http://localhost:<APP_PORT du .env ou 8080>)
#   WAIT_TIMEOUT (défaut : 180 secondes, délai maximal d'attente de l'API)
#
# Code de sortie : 0 = PERSISTANCE OK, 1 = PERSISTANCE KO (ou erreur).
# Le script n'affiche aucun secret et ne supprime jamais le volume.
# ============================================================
set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

PROJECT="uniflow"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-180}"

# Seul APP_PORT est lu dans .env (jamais les mots de passe).
if [[ -z "${API_URL:-}" ]]; then
  APP_PORT_ENV=""
  [[ -f .env ]] && APP_PORT_ENV="$(grep -E '^APP_PORT=' .env | tail -n1 | cut -d= -f2 | tr -d '\r[:space:]')"
  API_URL="http://localhost:${APP_PORT_ENV:-8080}"
fi

TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

log()  { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }
fail() { log "ERREUR : $*"; echo; echo "===== PERSISTANCE KO ====="; exit 1; }

# Attend que /actuator/health réponde "UP", avec un délai maximal.
wait_api() {
  local start=$SECONDS
  log "Attente de l'API sur ${API_URL}/actuator/health (max ${WAIT_TIMEOUT}s)..."
  until curl -fsS "${API_URL}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; do
    if (( SECONDS - start >= WAIT_TIMEOUT )); then
      docker compose ps
      fail "l'API ne répond pas après ${WAIT_TIMEOUT}s (voir : docker compose logs app)"
    fi
    sleep 2
  done
  log "API prête en $(( SECONDS - start ))s."
}

# Conteneurs appartenant au projet Compose (filtre par label, pas par nom :
# le conteneur de test « uniflow-db » hors Compose est volontairement ignoré).
project_containers() {
  docker ps -a --filter "label=com.docker.compose.project=${PROJECT}" --format '{{.Names}} ({{.Status}})'
}

# Exécute une requête SQL dans le conteneur db (identifiants lus dans le conteneur).
psql_db() {
  docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tA -c "$1"' _ "$1"
}

echo "===== Vérification de la persistance PostgreSQL (IP-49) ====="

# --- 1. Démarrage ---------------------------------------------------------
log "Étape 1/6 : docker compose up -d --build"
docker compose up -d --build || fail "échec du démarrage de la stack"
wait_api

VOLUME="$(docker volume ls -q --filter "label=com.docker.compose.project=${PROJECT}" --filter "label=com.docker.compose.volume=db-data")"
[[ -n "$VOLUME" ]] || fail "volume nommé db-data introuvable"
log "Volume nommé utilisé : ${VOLUME}"

# --- 2. Création d'une donnée de test -------------------------------------
INTITULE="Test persistance $(date '+%Y-%m-%d %H:%M:%S')"
log "Étape 2/6 : création de la formation « ${INTITULE} »"
PAYLOAD=$(printf '{"intitule":"%s","description":"Donnee de test IP-49","duree":10,"capaciteMax":20,"dateDebut":"2026-12-01","dateFin":"2026-12-05","statut":"OUVERTE"}' "$INTITULE")
HTTP=$(curl -sS -o "$TMP_BODY" -w '%{http_code}' -X POST "${API_URL}/api/formations" \
         -H 'Content-Type: application/json' -d "$PAYLOAD")
[[ "$HTTP" == "201" ]] || fail "POST /api/formations a renvoyé ${HTTP} : $(cat "$TMP_BODY")"
FORMATION_ID="$(sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p' "$TMP_BODY")"
[[ -n "$FORMATION_ID" ]] || fail "identifiant absent de la réponse : $(cat "$TMP_BODY")"
log "Formation créée : HTTP ${HTTP}, id=${FORMATION_ID}"
log "Avant arrêt, en base : $(psql_db "SELECT id_formation || ' | ' || intitule FROM formation WHERE id_formation = ${FORMATION_ID}")"

# --- 3. Arrêt complet (SANS -v) -------------------------------------------
log "Étape 3/6 : docker compose down (sans -v)"
docker compose down || fail "échec de docker compose down"
REMAINING="$(project_containers)"
[[ -z "$REMAINING" ]] || fail "des conteneurs du projet existent encore : ${REMAINING}"
log "Aucun conteneur du projet '${PROJECT}' ne subsiste."
docker volume inspect "$VOLUME" >/dev/null 2>&1 || fail "le volume ${VOLUME} a disparu"
log "Le volume ${VOLUME} est toujours présent."

# --- 4. Redémarrage --------------------------------------------------------
log "Étape 4/6 : docker compose up -d"
docker compose up -d || fail "échec du redémarrage"
wait_api

# --- 5. Vérifications ------------------------------------------------------
log "Étape 5/6 : vérification après redémarrage"
RESULT=0

HTTP=$(curl -sS -o "$TMP_BODY" -w '%{http_code}' "${API_URL}/api/formations/${FORMATION_ID}")
if [[ "$HTTP" == "200" ]] && grep -qF "\"intitule\":\"${INTITULE}\"" "$TMP_BODY"; then
  log "API  : OK  -> GET /api/formations/${FORMATION_ID} = 200, intitulé identique"
else
  log "API  : KO  -> GET /api/formations/${FORMATION_ID} = ${HTTP} : $(cat "$TMP_BODY")"
  RESULT=1
fi

DB_ROW="$(psql_db "SELECT intitule FROM formation WHERE id_formation = ${FORMATION_ID}" | tr -d '\r')"
if [[ "$DB_ROW" == "$INTITULE" ]]; then
  log "psql : OK  -> ligne trouvée : ${FORMATION_ID} | ${DB_ROW}"
else
  log "psql : KO  -> ligne absente ou différente : '${DB_ROW}'"
  RESULT=1
fi

# --- 6. Nettoyage ----------------------------------------------------------
log "Étape 6/6 : nettoyage (route DELETE /api/formations/{id} disponible)"
HTTP=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "${API_URL}/api/formations/${FORMATION_ID}")
if [[ "$HTTP" == "204" ]]; then
  log "Formation de test ${FORMATION_ID} supprimée (HTTP 204)."
else
  log "Attention : suppression de la formation ${FORMATION_ID} impossible (HTTP ${HTTP})."
fi

echo
if (( RESULT == 0 )); then
  echo "===== PERSISTANCE OK ====="
else
  echo "===== PERSISTANCE KO ====="
fi
exit "$RESULT"
