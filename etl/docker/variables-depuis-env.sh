#!/bin/bash
# Lancé avant Hop dans les conteneurs etl (HOP_CUSTOM_ENTRYPOINT_EXTENSION_SHELL_FILE_PATH) et hop-web (CMD du Dockerfile).
# Écrit les identifiants reçus du .env (via docker-compose) dans un fichier d'environnement Hop
# temporaire, propre au conteneur : aucun mot de passe dans les fichiers versionnés ni dans les journaux.

SECRETS_FILE=/tmp/docker-secrets.json

# log() est fourni par le script de démarrage de apache/hop, pas par celui de apache/hop-web
type log >/dev/null 2>&1 || log() { echo "$(date "+%Y/%m/%d %H:%M:%S") - $1"; }

cat > "${SECRETS_FILE}" <<JSON
{
  "variables": [
    { "name": "MYSQL_DB",       "value": "${MYSQL_DATABASE:-}" },
    { "name": "MYSQL_USER",     "value": "${MYSQL_USER:-}" },
    { "name": "MYSQL_PASSWORD", "value": "${MYSQL_PASSWORD:-}" },
    { "name": "PG_DB",          "value": "${POSTGRES_DB:-}" },
    { "name": "PG_USER",        "value": "${POSTGRES_USER:-}" },
    { "name": "PG_PASSWORD",    "value": "${POSTGRES_PASSWORD:-}" }
  ]
}
JSON

export HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS="${HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS},${SECRETS_FILE}"
log "Identifiants des bases chargés depuis les variables du conteneur (${SECRETS_FILE})"
