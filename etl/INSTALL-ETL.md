# Installation et exécution de l'ETL (Apache Hop)

L'ETL lit les produits bruts de MySQL (`produits_source`), contrôle les valeurs obligatoires, normalise les catégories et charge le résultat dans PostgreSQL : `produits`, et `produits_rejets` pour les lignes refusées.

**Rien à installer à la main** : Apache Hop tourne dans Docker, avec les images officielles `apache/hop` (exécution) et `apache/hop-web` (interface graphique dans le navigateur). Le pilote MySQL est ajouté automatiquement à la construction des images.

| Service | Rôle | Commande | Port sur la machine |
|---|---|---|---|
| `etl` | Exécute le workflow puis s'arrête (test de bout en bout, chargement des données) | `docker compose run --rm etl` | aucun |
| `hop-web` | Interface Hop dans le navigateur : voir, modifier et lancer les pipelines (démo, captures d'écran) | `docker compose --profile hop-web up -d hop-web` | `HOP_WEB_PORT` (8090) |

## Versions vérifiées

| Élément | Version | Lien |
|---|---|---|
| Apache Hop | **2.18.1** (juin 2026) | https://hop.apache.org/download/ |
| Images Docker | `apache/hop:2.18.1`, `apache/hop-web:2.18.1` | https://hub.docker.com/r/apache/hop |
| Pilote JDBC MySQL | `mysql-connector-j` **9.3.0** (téléchargé depuis Maven Central à la construction) | https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.3.0/ |
| Pilote JDBC PostgreSQL | 42.7.11 (fourni avec Hop) | — |

Vérifié le 28/10/2026 sous Windows 11 avec Docker 29.3 : 40 lignes lues, 29 produits chargés, 10 rejets.

## Ports : pas de conflit avec l'API

- `etl` ne publie **aucun port** : il lance `hop-run`, pas de serveur.
- `hop-web` écoute sur 8080 **dans son conteneur**, mais il est publié sur `HOP_WEB_PORT` (**8090** par défaut), alors que l'API utilise `API_PORT` (8080). Les deux peuvent tourner en même temps.
- Si 8090 est déjà pris sur la machine, changer `HOP_WEB_PORT` dans `.env`.

---

## 1. Lancer l'ETL

Prérequis : les bases tournent (`docker compose up -d`, voir le README).

```bash
docker compose run --rm etl
```

Le service `etl` (profil `etl`, il ne démarre donc pas avec `docker compose up`) :

1. construit l'image (cible `run` de `etl/Dockerfile`) ;
2. monte le dossier `etl/` en lecture seule, puis enregistre le projet `produits-etl` et l'environnement `docker` (`config/docker-config.json` : hôtes `mysql` et `postgres`) ;
3. lit les identifiants des bases dans le `.env` grâce à `etl/docker/variables-depuis-env.sh`, sans les écrire dans un fichier versionné ni dans le journal ;
4. exécute `wf_etl_produits.hwf`, puis s'arrête.

Code de retour : **0** si l'ETL a réussi, **1** sinon. C'est cette commande qu'utilise le test de bout en bout.

Pour afficher aussi chaque ligne rejetée avec son motif :

```bash
docker compose run --rm -e HOP_LOG_LEVEL=Detailed etl
```

## 2. Ouvrir les pipelines dans Hop Web (optionnel)

```bash
docker compose --profile hop-web up -d hop-web
```

Puis ouvrir **http://localhost:8090/ui** (la première ouverture prend environ 30 secondes).

1. Hop Web s'ouvre directement sur le projet **produits-etl** et l'environnement **docker** (affichés en bas à gauche). S'il affiche `default`, ouvrir de nouveau l'adresse `http://localhost:8090/ui` (sans paramètres), ou cliquer sur `default` et choisir `produits-etl`.
2. Pour voir les fichiers : icône *dossier* (Explorateur) dans la barre de gauche ; l'icône *graphique* sert à l'historique des exécutions (`local-audit`).
3. Ouvrir `wf_etl_produits.hwf`, `pl_produits.hpl` ou `pl_bilan.hpl` (double-clic dans l'Explorateur, ou icône *Ouvrir*).
4. Lancer avec le bouton *Run* (▶), configuration `local`. Le journal et les compteurs par transform s'affichent en bas : c'est la vue à capturer pour la présentation.
5. Les connexions `mysql_source` et `postgres_cible` sont dans *Metadata → Relational Database Connection* ; le bouton *Test* doit répondre OK.

Le dossier `etl/` est monté en lecture/écriture : une modification enregistrée dans Hop Web arrive directement dans le dépôt.

Arrêter Hop Web :

```bash
docker compose --profile hop-web stop hop-web
```

---

## 3. Contenu du dossier `etl/`

| Fichier | Rôle |
|---|---|
| `hop-projet/wf_etl_produits.hwf` | Workflow : vérification des connexions → `pl_produits` → `pl_bilan` → journal de succès ; en cas d'erreur : journal d'échec puis arrêt en erreur |
| `hop-projet/pl_produits.hpl` | Pipeline d'extraction, de contrôle, de normalisation et de chargement |
| `hop-projet/pl_bilan.hpl` | Compte les lignes lues, chargées et rejetées et les transmet au workflow |
| `hop-projet/metadata/rdbms/` | Connexions `mysql_source` et `postgres_cible` (variables uniquement, aucun mot de passe) |
| `hop-projet/config/docker-config.json` | Environnement `docker` : hôtes et ports du réseau Docker, chemin du référentiel |
| `referentiel/categories.csv` | Référentiel variante → catégorie officielle |
| `Dockerfile` | Images `run` (hop-run) et `web` (Hop Web), avec le pilote MySQL |
| `docker/variables-depuis-env.sh` | Transmet à Hop les identifiants du `.env` (base, utilisateur, mot de passe) |
| `docker/demarrer-hop-web.sh` | Démarrage de Hop Web : identifiants, projet `produits-etl` et environnement `docker` par défaut |

### Variables utilisées par les connexions

| Variable | Source |
|---|---|
| `MYSQL_HOST`, `MYSQL_PORT`, `PG_HOST`, `PG_PORT`, `CATEGORIES_CSV` | `config/docker-config.json` |
| `MYSQL_DB`, `MYSQL_USER`, `MYSQL_PASSWORD`, `PG_DB`, `PG_USER`, `PG_PASSWORD` | `.env` (`MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`) |

Les conteneurs Hop se connectent aux bases par le réseau Docker (`mysql:3306`, `postgres:5432`). Les ports publiés sur la machine (`MYSQL_PORT`, `PG_PORT` du `.env`) n'ont donc aucune influence sur l'ETL.

---

## 4. Fonctionnement du pipeline `pl_produits`

```
[Lire produits_source] (mysql_source)
        |
[Normaliser nom et catégorie]   nom : trim ; categorie_cle : trim + minuscules + sans accents
        |
[Chercher catégorie officielle] <-- [Lire categories.csv]
        |
[Contrôler les valeurs]         motif = liste des erreurs (vide si la ligne est valide)
        |
[Ligne valide ?]  (motif IS NULL)
   |-- oui --> [Préparer produit] --> [Insérer / mettre à jour produits]   clé : nom + categorie
   |-- non --> [Préparer rejet] --> [Journaliser rejet] --> [Écrire produits_rejets]
```

Règles appliquées :

- **Valeurs obligatoires** : `nom`, `prix` et `categorie` non vides (un nom fait uniquement d'espaces compte comme vide), `prix >= 0` et `quantite >= 0`. `prix = 0` et `quantite = 0` sont acceptés.
- **Catégories** : la valeur est mise en minuscules, débarrassée des espaces autour et des accents (`' ÉLECTRONIQUE '` devient `electronique`), puis recherchée dans `categories.csv`. Une **catégorie inconnue est rejetée** (décision du groupe), avec le motif `catégorie inconnue (jouets)`.
- **Motif précis** : toutes les erreurs d'une ligne sont listées, par exemple `nom manquant, prix manquant`.
- **Pas de doublon** : *Insert / update* sur la clé fonctionnelle `nom + categorie`. Une ligne existante est mise à jour (description, prix, quantité) ; sinon elle est insérée. `date_creation` reste celle de la première insertion.
- **Relançable** : `produits_rejets` est vidée au début de chaque exécution et reflète donc le dernier passage. Relancer l'ETL ne crée pas de doublon dans `produits`.

## 5. Résultat obtenu

Avec le jeu de test de `db/` (détail dans `db/DONNEES-TEST.md`) :

```
==================== ETL produits : SUCCÈS ====================
Lignes lues dans MySQL (produits_source)    : 40
Produits dans PostgreSQL (produits)         : 29
Lignes rejetées (produits_rejets)           : 10
================================================================
```

Sur les 40 lignes lues, 30 sont valides et 10 sont rejetées. Les 30 lignes valides donnent 29 produits, car la ligne 30 (doublon de « Souris sans fil ») met à jour la ligne existante. Répartition : Electronique 7, Informatique 6, Vetements 6, Alimentation 5, Maison 5.

| id source | Motif |
|---|---|
| 31, 32, 33 | nom manquant |
| 34 | prix manquant |
| 35 | prix négatif |
| 36 | quantité négative |
| 37, 38 | catégorie manquante |
| 39 | catégorie inconnue (jouets) |
| 40 | nom manquant, prix manquant |

Pour vérifier dans PostgreSQL :

```bash
docker compose exec postgres psql -U produits_user -d produits_db -c "SELECT categorie, COUNT(*) FROM produits GROUP BY 1 ORDER BY 1"
docker compose exec postgres psql -U produits_user -d produits_db -c "SELECT * FROM produits_rejets ORDER BY id_source"
```

## 6. Dépannage

| Problème | Solution |
|---|---|
| `Access denied for user 'etl_user'` | Le mot de passe du `.env` a changé après le premier démarrage de MySQL : `docker compose down -v` puis `docker compose up -d` |
| `Communications link failure` / `Connection refused` | Les bases sont arrêtées ou pas encore prêtes : `docker compose ps` (elles doivent être `healthy`) |
| `port is already allocated` au démarrage de `hop-web` | Changer `HOP_WEB_PORT` dans `.env` (il doit être différent de `API_PORT`) |
| Hop Web affiche une page blanche | Attendre 30 secondes après le démarrage, puis recharger http://localhost:8090/ui |
| Les fichiers modifiés dans Hop Web n'apparaissent pas dans `etl/` | Enregistrer depuis Hop Web (Ctrl+S) : seul le dossier `/files` du conteneur correspond à `etl/` |
| Une modification de `etl/Dockerfile` n'est pas prise en compte | `docker compose --profile etl --profile hop-web build etl hop-web` |
