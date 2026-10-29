#!/usr/bin/env bash
# =====================================================================
# Test de bout en bout de la chaîne principale :
#   MySQL (source) -> ETL Apache Hop -> PostgreSQL -> API REST -> Prometheus
#
# Usage (depuis la racine du projet) :
#   bash tests/e2e.sh            # utilise les services déjà démarrés (ou les démarre)
#   bash tests/e2e.sh --clean    # repart de zéro : docker compose down -v puis up --build
#
# Code de retour : 0 si tous les contrôles réussissent, 1 sinon.
# Résultats attendus : voir tests/RESULTATS-ATTENDUS.md
# =====================================================================
set -uo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "ERREUR : fichier .env introuvable (faire : cp .env.example .env)"; exit 1
fi
set -a; . ./.env; set +a

API="http://localhost:${API_PORT:-8080}"
PROM="http://localhost:${PROMETHEUS_PORT:-9090}"
NB_SOURCE=40; NB_CHARGES=29; NB_REJETS=10

LOGS=$(mktemp -d)   # journaux de l'ETL
OK=0; KO=0
ok()   { OK=$((OK+1)); echo "  [OK]  $1"; }
ko()   { KO=$((KO+1)); echo "  [KO]  $1"; }
check() { # check "libellé" "attendu" "obtenu"
  if [ "$2" = "$3" ]; then ok "$1 (= $3)"; else ko "$1 : attendu '$2', obtenu '$3'"; fi
}
etape() { echo; echo "== $1"; }

# Appelle l'API : écrit le corps dans $BODY et le code HTTP dans $CODE
BODY=""; CODE=""
appel() { # appel METHODE URL [corps JSON]
  local out
  if [ $# -ge 3 ]; then
    out=$(curl -s -w $'\n%{http_code}' -X "$1" -H 'Content-Type: application/json' -d "$3" "$2")
  else
    out=$(curl -s -w $'\n%{http_code}' -X "$1" "$2")
  fi
  CODE=${out##*$'\n'}; BODY=${out%$'\n'*}
}
json_int() { echo "$BODY" | grep -o "\"$1\":[0-9]*" | head -1 | cut -d: -f2; }
json_str() { echo "$BODY" | grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
psql_val() { docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tA -c "$1" 2>/dev/null | tr -d '[:space:]'; }
mysql_val() { docker compose exec -T mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -N -B "$MYSQL_DATABASE" -e "$1" 2>/dev/null | tr -d '[:space:]'; }

# ---------------------------------------------------------------------
etape "0. Démarrage des services"
if [ "${1:-}" = "--clean" ]; then
  docker compose down -v >/dev/null 2>&1
fi
docker compose up -d --build >/dev/null 2>&1 || { ko "docker compose up a échoué"; exit 1; }

for i in $(seq 1 60); do
  s=$(curl -s "$API/actuator/health" | grep -o '"status":"UP"' | head -1)
  [ -n "$s" ] && break
  sleep 3
done
appel GET "$API/actuator/health"
check "API démarrée (GET /actuator/health)" "200" "$CODE"

# ---------------------------------------------------------------------
etape "1. Source MySQL"
check "lignes dans produits_source" "$NB_SOURCE" "$(mysql_val 'SELECT COUNT(*) FROM produits_source;')"

# ---------------------------------------------------------------------
etape "2. ETL Apache Hop (MySQL -> PostgreSQL)"
if docker compose run --rm etl >"$LOGS/etl.log" 2>&1; then
  ok "workflow Hop terminé avec le code 0"
else
  ko "workflow Hop en erreur (journal : $LOGS/etl.log)"; tail -15 "$LOGS/etl.log"
fi
check "produits chargés dans PostgreSQL" "$NB_CHARGES" "$(psql_val 'SELECT COUNT(*) FROM produits;')"
check "lignes rejetées avec un motif" "$NB_REJETS" "$(psql_val 'SELECT COUNT(*) FROM produits_rejets;')"
check "rejets sans motif" "0" "$(psql_val "SELECT COUNT(*) FROM produits_rejets WHERE motif IS NULL OR motif = '';")"
check "catégories hors référentiel dans produits" "0" \
  "$(psql_val "SELECT COUNT(*) FROM produits WHERE categorie NOT IN ('Electronique','Informatique','Vetements','Alimentation','Maison');")"
check "prix négatifs dans produits" "0" "$(psql_val 'SELECT COUNT(*) FROM produits WHERE prix < 0;')"

# Idempotence : relancer l'ETL ne crée aucun doublon
docker compose run --rm etl >"$LOGS/etl-relance.log" 2>&1
check "relance de l'ETL : toujours le même nombre de produits" "$NB_CHARGES" "$(psql_val 'SELECT COUNT(*) FROM produits;')"

# ---------------------------------------------------------------------
etape "3. API REST : lecture des données chargées par l'ETL"
appel GET "$API/api/v1/produits?size=100"
check "GET /api/v1/produits" "200" "$CODE"
check "totalElements = produits chargés" "$NB_CHARGES" "$(json_int totalElements)"

appel GET "$API/api/v1/produits?categorie=informatique"
check "filtre catégorie (casse ignorée)" "200" "$CODE"
NB_INFO=$(psql_val "SELECT COUNT(*) FROM produits WHERE categorie = 'Informatique';")
check "totalElements du filtre = lignes SQL" "$NB_INFO" "$(json_int totalElements)"

# ---------------------------------------------------------------------
etape "4. API REST : création, modification, suppression"
NEW='{"nom":"Produit E2E","description":"cree par tests/e2e.sh","prix":12.50,"quantite":3,"categorie":"Maison"}'
appel POST "$API/api/v1/produits" "$NEW"
check "POST -> 201" "201" "$CODE"
ID=$(json_int id)
[ -n "$ID" ] && ok "id attribué ($ID)" || ko "pas d'id dans la réponse"

check "le produit est dans PostgreSQL" "1" "$(psql_val "SELECT COUNT(*) FROM produits WHERE id = ${ID:-0};")"

appel GET "$API/api/v1/produits/$ID"
check "GET /{id} -> 200" "200" "$CODE"

UPD='{"nom":"Produit E2E modifie","description":"modifie","prix":20.00,"quantite":7,"categorie":"Maison"}'
appel PUT "$API/api/v1/produits/$ID" "$UPD"
check "PUT -> 200" "200" "$CODE"
check "nom modifié en base" "ProduitE2Emodifie" "$(psql_val "SELECT nom FROM produits WHERE id = ${ID:-0};")"

appel DELETE "$API/api/v1/produits/$ID"
check "DELETE -> 200" "200" "$CODE"
appel GET "$API/api/v1/produits/$ID"
check "GET après suppression -> 404" "404" "$CODE"
check "produit absent de PostgreSQL" "0" "$(psql_val "SELECT COUNT(*) FROM produits WHERE id = ${ID:-0};")"
check "retour à $NB_CHARGES produits" "$NB_CHARGES" "$(psql_val 'SELECT COUNT(*) FROM produits;')"

# ---------------------------------------------------------------------
etape "5. API REST : erreurs"
appel POST "$API/api/v1/produits" '{"nom":"","prix":-5,"quantite":-1,"categorie":""}'
check "corps invalide -> 400" "400" "$CODE"
check "message d'erreur standard" "Données invalides" "$(json_str message)"
appel GET "$API/api/v1/produits/999999"
check "id inexistant -> 404" "404" "$CODE"
appel GET "$API/api/v1/produits/abc"
check "id non numérique -> 400" "400" "$CODE"
appel GET "$API/api/v1/produits?page=-1&size=500"
check "pagination invalide -> 400" "400" "$CODE"

# ---------------------------------------------------------------------
etape "6. Supervision : Actuator et Prometheus"
appel GET "$API/actuator/prometheus"
check "GET /actuator/prometheus" "200" "$CODE"
echo "$BODY" | grep -q 'http_server_requests_seconds_count' && ok "métrique http_server_requests exposée" || ko "métrique http_server_requests absente"

UP=""
for i in $(seq 1 12); do   # Prometheus collecte toutes les 5 s
  UP=$(curl -s -G "$PROM/api/v1/query" --data-urlencode 'query=up{job="produits-api"}' \
        | grep -o '"value":\[[^]]*\]' | grep -o '"[01]"' | tr -d '"')
  [ "$UP" = "1" ] && break
  sleep 3
done
check "Prometheus : cible produits-api (up)" "1" "$UP"

NB_REQ=0
for i in $(seq 1 12); do
  NB_REQ=$(curl -s -G "$PROM/api/v1/query" \
    --data-urlencode 'query=sum(http_server_requests_seconds_count{job="produits-api"})' \
    | grep -o '"value":\[[^]]*\]' | grep -o '"[0-9.]*"' | tail -1 | tr -d '"')
  [ -n "$NB_REQ" ] && [ "${NB_REQ%.*}" -gt 0 ] 2>/dev/null && break
  sleep 3
done
if [ -n "$NB_REQ" ] && [ "${NB_REQ%.*}" -gt 0 ] 2>/dev/null; then
  ok "Prometheus a collecté les requêtes HTTP de l'API (${NB_REQ%.*} requêtes)"
else
  ko "Prometheus n'a collecté aucune requête HTTP"
fi

# ---------------------------------------------------------------------
echo
echo "=============================================================="
echo " Résultat : $OK contrôles réussis, $KO échec(s)"
echo "=============================================================="
[ "$KO" -eq 0 ]
