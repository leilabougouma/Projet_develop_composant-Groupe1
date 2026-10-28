#!/bin/bash
# Démarrage du conteneur hop-web :
#   1. identifiants des bases depuis le .env (variables-depuis-env.sh)
#   2. enregistrement du projet produits-etl et de l'environnement docker
#   3. produits-etl / docker deviennent les choix par défaut, pour que Hop Web s'ouvre directement dessus
#   4. démarrage normal de Hop Web (script officiel de l'image)

source /files/docker/variables-depuis-env.sh

HOP_CONF="${DEPLOYMENT_PATH}/hop-conf.sh"

if ! "${HOP_CONF}" -pl 2>/dev/null | grep -q -E "^  ${HOP_PROJECT_NAME} :"; then
  "${HOP_CONF}" --project="${HOP_PROJECT_NAME}" --project-create \
    --project-home="${HOP_PROJECT_FOLDER}" \
    --project-config-file="${HOP_PROJECT_CONFIG_FILE_NAME}" --project-keep-config-file
fi

if ! "${HOP_CONF}" -el 2>/dev/null | grep -q -E -x "^  ${HOP_ENVIRONMENT_NAME}"; then
  "${HOP_CONF}" --environment="${HOP_ENVIRONMENT_NAME}" --environment-create \
    --environment-project="${HOP_PROJECT_NAME}" \
    --environment-purpose="Apache Hop docker container" \
    --environment-config-files="${HOP_ENVIRONMENT_CONFIG_FILE_NAME_PATHS}"
fi

"${HOP_CONF}" --default-project="${HOP_PROJECT_NAME}" --default-environment="${HOP_ENVIRONMENT_NAME}"
log "Projet par défaut : ${HOP_PROJECT_NAME}, environnement par défaut : ${HOP_ENVIRONMENT_NAME}"

exec /bin/bash /tmp/run-web.sh
