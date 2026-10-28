# API REST de produits — Spring Boot, Apache Hop (ETL) et Prometheus

Projet « Développement à base de composants et services web » — M1 IBAM 2026 — Exercice 1.

> Les sections *Architecture* et *Supervision* seront complétées par les autres membres du groupe. Les sections *ETL* et *API* sont décrites ci-dessous.

## Prérequis

| Outil | Version minimale | Utilisé pour | Vérifier avec |
|---|---|---|---|
| Docker Desktop (Windows/macOS) ou Docker Engine (Linux) | 24+ | Bases MySQL et PostgreSQL (puis API et Prometheus) | `docker --version` |
| Docker Compose (inclus dans Docker Desktop) | v2 | Lancement de tous les services | `docker compose version` |
| Java (JDK) | 17 ou 21 | API Spring Boot (hors Docker) | `java -version` |
| Maven | 3.9+ | Compilation de l'API | `mvn -version` |
| Apache Hop | 2.18.1 (images Docker, rien à installer) | ETL MySQL → PostgreSQL (voir `etl/INSTALL-ETL.md`) | — |
| Postman | récent | Tests de l'API | — |
| Git | récent | Récupération du dépôt | `git --version` |

### Ports utilisés

| Service | Port sur la machine | Variable dans `.env` |
|---|---|---|
| MySQL (source) | 3306 | `MYSQL_PORT` |
| PostgreSQL (cible) | 5432 | `PG_PORT` |
| API Spring Boot | 8080 | `API_PORT` |
| Prometheus | 9090 | `PROMETHEUS_PORT` |
| Hop Web (interface de l'ETL, optionnelle) | 8090 | `HOP_WEB_PORT` |

Le service `etl` ne publie aucun port, et Hop Web est publié sur 8090 : aucun conflit avec l'API sur 8080.

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
| `port is already allocated` | Changer `MYSQL_PORT`, `PG_PORT` ou `API_PORT` dans `.env`, puis `docker compose up -d` (ou arrêter le programme qui occupe le port, ex. `docker stop <conteneur>`) |
| Les tables ou les données n'apparaissent pas | Les scripts ne s'exécutent qu'au premier démarrage : faire `docker compose down -v` puis `docker compose up -d` |
| Accents mal affichés dans MySQL | Se connecter avec `--default-character-set=utf8mb4` |
| Service `unhealthy` | Regarder `docker compose logs <service>` (souvent une variable manquante dans `.env`) |

## ETL (Apache Hop)

### Pourquoi Apache Hop plutôt que Talend Open Studio

L'énoncé prévoit Talend Open Studio, mais cet outil a été abandonné début 2024 : il n'est plus maintenu et son installation n'est plus fiable. L'énoncé autorise une autre solution ETL dont la disponibilité a été vérifiée ; le groupe a retenu **Apache Hop 2.18.1** :

- projet open source actif de la fondation Apache, issu de Kettle (Pentaho), avec beaucoup de documentation ;
- pipelines (`.hpl`) et workflows (`.hwf`) en XML, donc versionnés dans Git avec le reste du projet ;
- exécution en ligne de commande (`hop-run`) et image Docker officielle (`apache/hop`), donc reproductible et utilisable par le test de bout en bout.

### Lancer l'ETL

Les bases doivent être démarrées (`docker compose up -d`). Ensuite :

```bash
docker compose run --rm etl
```

Résultat attendu à la fin du journal : **40 lignes lues, 29 produits dans PostgreSQL, 10 lignes rejetées** (30 lignes valides, dont un doublon qui met à jour la ligne existante). L'ETL peut être relancé sans créer de doublon. Le code de retour vaut 0 en cas de succès et 1 en cas d'échec.

Pour voir et lancer les pipelines dans l'interface graphique de Hop, sans rien installer :

```bash
docker compose --profile hop-web up -d hop-web    # puis http://localhost:8090/ui
```

Détails (projet `produits-etl`, environnement `docker`, dépannage) : [etl/INSTALL-ETL.md](etl/INSTALL-ETL.md). Démarche et résultats : [etl/RAPPORT-ETL.md](etl/RAPPORT-ETL.md).

### Ce que fait l'ETL

| Étape | Détail |
|---|---|
| Extraction | `SELECT` sur `produits_source` (MySQL, connexion `mysql_source`) |
| Contrôle | `nom`, `prix`, `categorie` non vides ; `prix >= 0` ; `quantite >= 0` |
| Normalisation | Catégorie en minuscules, sans espaces ni accents, puis correspondance avec [etl/referentiel/categories.csv](etl/referentiel/categories.csv) (ex. `electro` et `ÉLECTRONIQUE` donnent `Electronique`) ; une catégorie inconnue est **rejetée** |
| Chargement | *Insert / update* dans `produits` (PostgreSQL, connexion `postgres_cible`) sur la clé `nom + categorie` |
| Rejets | `produits_rejets` avec un motif précis (`prix négatif`, `catégorie inconnue (jouets)`, `nom manquant, prix manquant`…), vidée à chaque exécution |
| Journal | Nombre de lignes lues, chargées et rejetées à la fin du workflow |

Aucun mot de passe n'est écrit dans les fichiers Hop versionnés : les connexions utilisent des variables (`${MYSQL_PASSWORD}`, `${PG_PASSWORD}`…), fournies par le `.env` au démarrage des conteneurs `etl` et `hop-web`.

## API REST (Spring Boot)

Code dans `api/` : Spring Boot 3.3 / Java 17, architecture en couches `entity`, `repository`, `service`, `controller`, avec des DTO en entrée et en sortie. Le schéma est créé par les scripts SQL de `db/` (`ddl-auto: validate`), pas par Hibernate.

### Démarrage

```bash
cp .env.example .env              # si ce n'est pas déjà fait
docker compose up -d --build      # démarre MySQL, PostgreSQL et l'API
curl http://localhost:8080/actuator/health     # {"status":"UP"}
```

| URL | Rôle |
|---|---|
| `http://localhost:8080/swagger-ui/index.html` | Documentation interactive (`/` redirige vers cette page) |
| `http://localhost:8080/v3/api-docs` | Contrat OpenAPI généré à la volée |
| `http://localhost:8080/actuator/health` | État de l'application |

L'API lit `DB_HOST` (par défaut `localhost`), `POSTGRES_USER` et `POSTGRES_PASSWORD`. Dans Docker, `DB_HOST=postgres` est fourni par `docker-compose.yml`. Pour lancer l'API hors Docker, il faut exporter ces variables, puis `cd api && mvn spring-boot:run`.

### Endpoints

| Opération | Méthode | URL | Succès | Erreurs |
|---|---|---|---|---|
| Lister (pagination, filtre catégorie) | GET | `/api/v1/produits?categorie=&page=0&size=20` | 200 | 400 si `page` < 0 ou `size` hors 1 à 100 |
| Consulter | GET | `/api/v1/produits/{id}` | 200 | 404 |
| Créer | POST | `/api/v1/produits` | 201 + en-tête `Location` | 400 |
| Modifier | PUT | `/api/v1/produits/{id}` | 200 | 400, 404 |
| Supprimer | DELETE | `/api/v1/produits/{id}` | 204 | 404 |

- Le filtre `categorie` ignore la casse. Les résultats sont triés par `id`.
- La liste renvoie `{content, page, size, totalElements, totalPages}`.
- `dateCreation` est posée par la base à la création et n'est jamais modifiée par un `PUT`.

Validation du corps (`nom` et `categorie` non vides, `prix` >= 0, `quantite` >= 0) :

```bash
curl -X POST http://localhost:8080/api/v1/produits -H 'Content-Type: application/json' \
  -d '{"nom":"Clavier","description":"AZERTY","prix":59.90,"quantite":25,"categorie":"Informatique"}'
```

Toutes les erreurs ont le même format JSON (gestion centralisée dans `GlobalExceptionHandler`), y compris une route inconnue :

```json
{"timestamp":"...","status":400,"error":"Bad Request","message":"Données invalides",
 "path":"/api/v1/produits","details":[{"champ":"prix","message":"Le prix doit être positif ou nul"}]}
```

### Contrat OpenAPI

Le contrat `api/openapi.yaml` est généré par springdoc puis exporté dans le dépôt. Pour le régénérer après une modification de l'API :

```bash
cd api && mvn test -Dtest=OpenApiExportTest -Dopenapi.export=true
```

### Tests

```bash
cd api && mvn test
```

28 tests (JUnit 5) : tests unitaires du service (Mockito) et tests d'intégration MockMvc de bout en bout (contrôleur, service, JPA, base **H2 en mode PostgreSQL**, avec le schéma de `api/src/test/resources/schema-test.sql`). Ils n'ont pas besoin de Docker. Testcontainers n'est pas utilisé (optionnel).

### Modifications apportées à l'environnement

- `docker-compose.yml` : le service `api` (build de `./api`, port `API_PORT`, attend que PostgreSQL soit `healthy`) est activé.
- Fichiers ajoutés dans `api/` : `pom.xml`, `Dockerfile` (build Maven puis image JRE 17, utilisateur non-root), `.dockerignore`, `openapi.yaml`, `src/`.
