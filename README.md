# API REST de produits — Spring Boot, Apache Hop (ETL) et Prometheus

Projet « Développement à base de composants et services web » — M1 IBAM 2026 — Exercice 1.

> Les sections *Architecture*, *ETL*, *API*, *Supervision* et *Tests* seront complétées par les autres membres du groupe.

## Prérequis

| Outil | Version minimale | Utilisé pour | Vérifier avec |
|---|---|---|---|
| Docker Desktop (Windows/macOS) ou Docker Engine (Linux) | 24+ | Bases MySQL et PostgreSQL (puis API et Prometheus) | `docker --version` |
| Docker Compose (inclus dans Docker Desktop) | v2 | Lancement de tous les services | `docker compose version` |
| Java (JDK) | 17 ou 21 | API Spring Boot et Apache Hop | `java -version` |
| Maven | 3.9+ | Compilation de l'API | `mvn -version` |
| Apache Hop | dernière version stable | ETL MySQL → PostgreSQL (installation manuelle, voir `etl/INSTALL-ETL.md`) | — |
| Postman | récent | Tests de l'API | — |
| Git | récent | Récupération du dépôt | `git --version` |

### Ports utilisés

| Service | Port sur la machine | Variable dans `.env` |
|---|---|---|
| MySQL (source) | 3306 | `MYSQL_PORT` |
| PostgreSQL (cible) | 5432 | `PG_PORT` |
| API Spring Boot | 8080 | `API_PORT` |
| Prometheus | 9090 | `PROMETHEUS_PORT` |

Si un port est déjà pris (par exemple un MySQL déjà installé sur la machine), changer la valeur dans `.env` (ex. `MYSQL_PORT=3307`).

## Installation des bases de données

```bash
# 1. Récupérer le projet
git clone <url-du-depot>
cd projet-produits

# 2. Créer le fichier de configuration (puis changer les mots de passe si besoin)
cp .env.example .env          # Windows : copy .env.example .env

# 3. Démarrer MySQL et PostgreSQL
docker compose up -d

# 4. Attendre que les deux services soient "healthy" (30 s environ)
docker compose ps

# 5. Vérifier les bases (Git Bash sous Windows)
bash db/verifier-bases.sh
```

Résultat attendu : **40 lignes** dans `produits_source` (MySQL) et des tables `produits` et `produits_rejets` **vides** dans PostgreSQL (elles sont remplies par l'ETL).

### Contenu du dossier `db/`

| Fichier | Rôle |
|---|---|
| `db/mysql/01-init-mysql.sql` | Crée la table source `produits_source` |
| `db/mysql/02-donnees-test-mysql.sql` | Insère 40 produits de test (valides, invalides, doublon, catégories mal écrites) |
| `db/postgres/init-postgres.sql` | Crée les tables cibles `produits` et `produits_rejets` |
| `db/DONNEES-TEST.md` | Détail du jeu de test et résultats attendus de l'ETL |
| `db/verifier-bases.sh` | Vérifie que les deux bases répondent et compte les lignes |

Les scripts SQL sont exécutés **automatiquement au premier démarrage** des conteneurs, par ordre alphabétique (d'où les préfixes `01-`, `02-`).

### Paramètres de connexion (pour l'ETL et l'API)

| | MySQL (source) | PostgreSQL (cible) |
|---|---|---|
| Hôte depuis la machine (Hop, DBeaver…) | `localhost` | `localhost` |
| Hôte depuis un autre conteneur (API) | `mysql` | `postgres` |
| Port | `3306` | `5432` |
| Base | `produits_source_db` | `produits_db` |
| Utilisateur / mot de passe | `MYSQL_USER` / `MYSQL_PASSWORD` du `.env` | `POSTGRES_USER` / `POSTGRES_PASSWORD` du `.env` |

### Commandes utiles

```bash
docker compose logs mysql postgres      # voir les journaux
docker compose down                     # arrêter (les données sont conservées)
docker compose down -v                  # arrêter ET effacer les données (réinitialise les bases)

# Ouvrir une console SQL
docker compose exec mysql mysql -u etl_user -p produits_source_db
docker compose exec postgres psql -U produits_user -d produits_db
```

### Dépannage

| Problème | Solution |
|---|---|
| `port is already allocated` | Changer `MYSQL_PORT` ou `PG_PORT` dans `.env`, puis `docker compose up -d` |
| Les tables ou les données n'apparaissent pas | Les scripts ne s'exécutent qu'au premier démarrage : faire `docker compose down -v` puis `docker compose up -d` |
| Accents mal affichés dans MySQL | Se connecter avec `--default-character-set=utf8mb4` |
| Service `unhealthy` | Regarder `docker compose logs <service>` (souvent une variable manquante dans `.env`) |
