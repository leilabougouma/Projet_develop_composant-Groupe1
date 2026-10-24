#!/usr/bin/env bash
# =====================================================================
# Vérifie que les deux bases sont prêtes et affiche un résumé.
# Usage (depuis la racine du projet) :   bash db/verifier-bases.sh
# Réutilisable dans le test de bout en bout (partie 5).
# =====================================================================
set -euo pipefail

# Charge les variables du .env
if [ -f .env ]; then
  set -a; . ./.env; set +a
else
  echo "ERREUR : fichier .env introuvable (faire : cp .env.example .env)"; exit 1
fi

echo "=== MySQL (source) : ${MYSQL_DATABASE}.produits_source ==="
docker compose exec -T mysql mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" \
  --default-character-set=utf8mb4 "${MYSQL_DATABASE}" -e "
  SELECT COUNT(*) AS nb_lignes_source FROM produits_source;
  SELECT categorie, COUNT(*) AS nb FROM produits_source GROUP BY categorie ORDER BY categorie;" 2>/dev/null

echo
echo "=== PostgreSQL (cible) : ${POSTGRES_DB} ==="
docker compose exec -T postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "
  SELECT 'produits' AS table, COUNT(*) AS nb FROM produits
  UNION ALL
  SELECT 'produits_rejets', COUNT(*) FROM produits_rejets;"

echo "OK : les deux bases répondent."
