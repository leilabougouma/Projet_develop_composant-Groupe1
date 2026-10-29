# Tests et résultats attendus

Quatre niveaux de tests, du plus rapide au plus complet.

| Niveau | Commande | Docker | Durée | Résultat attendu |
|---|---|---|---|---|
| 1. Tests unitaires et d'intégration (JUnit 5, H2) | `cd api && mvn test` | non | ~15 s | 27 tests réussis, 0 échec, 1 ignoré (export OpenAPI) |
| 2. Collection Postman | Import de `postman/API-Produits.postman_collection.json` puis Runner (ou `newman run ...`) | oui (API lancée) | ~1 s | 18 requêtes, 41 assertions, 0 échec |
| 3. Collection Bruno | Bruno : *Open Collection* → `bruno/`, environnement *Local*, Runner | oui (API lancée) | ~1 s | 18 requêtes, 0 échec |
| 4. **Test de bout en bout** | `bash tests/e2e.sh` | oui | 1 à 3 min | **32 contrôles réussis, 0 échec**, code de retour 0 |

Les niveaux 2 et 3 testent l'API seule. Le niveau 4 teste toute la chaîne principale du projet : MySQL → ETL Apache Hop → PostgreSQL → API REST → Prometheus.

## Test de bout en bout (`tests/e2e.sh`)

```bash
cp .env.example .env            # une seule fois
bash tests/e2e.sh --clean       # repart de zéro : docker compose down -v, puis up --build
bash tests/e2e.sh               # ou : réutilise les services déjà démarrés
```

Sous Windows, lancer le script dans **Git Bash**. Le script lit le `.env` (ports et mots de passe) et se termine par un code de retour 0 (succès) ou 1 (au moins un échec).

### Ce que le script vérifie, dans l'ordre

| Étape | Contrôle | Résultat attendu |
|---|---|---|
| 0. Démarrage | `docker compose up -d --build`, puis attente de `/actuator/health` | HTTP 200 |
| 1. Source | `SELECT COUNT(*) FROM produits_source` (MySQL) | **40** |
| 2. ETL | `docker compose run --rm etl` | code de retour 0 |
| | Lignes dans `produits` (PostgreSQL) | **29** |
| | Lignes dans `produits_rejets` | **10**, toutes avec un motif |
| | Catégories hors référentiel, prix négatifs dans `produits` | **0** |
| | Relance de l'ETL | toujours **29** produits (idempotence) |
| 3. Lecture | `GET /api/v1/produits?size=100` | 200, `totalElements` = **29** |
| | `GET /api/v1/produits?categorie=informatique` | 200, `totalElements` = nombre de lignes SQL (6) |
| 4. Écriture | `POST` | 201, produit présent dans PostgreSQL |
| | `GET /{id}` puis `PUT` | 200, nom modifié en base |
| | `DELETE` puis `GET /{id}` | 200, puis 404, produit absent de PostgreSQL |
| | Nombre de produits après le parcours | de nouveau **29** |
| 5. Erreurs | Corps invalide, id inexistant, id non numérique, pagination invalide | 400, 404, 400, 400 (format JSON d'erreur unique) |
| 6. Supervision | `GET /actuator/prometheus` | 200, contient `http_server_requests_seconds_count` |
| | Requête Prometheus `up{job="produits-api"}` | **1** |
| | Requête Prometheus `sum(http_server_requests_seconds_count)` | supérieure à 0 |

### Sortie attendue (extrait)

```
== 2. ETL Apache Hop (MySQL -> PostgreSQL)
  [OK]  workflow Hop terminé avec le code 0
  [OK]  produits chargés dans PostgreSQL (= 29)
  [OK]  lignes rejetées avec un motif (= 10)
  ...
==============================================================
 Résultat : 32 contrôles réussis, 0 échec(s)
==============================================================
```

Dernière exécution validée : 29/10/2026, depuis un état vierge (`--clean`), 32 contrôles réussis.

## Tests Java (`mvn test`)

| Classe | Tests | Contenu |
|---|---|---|
| `ProduitServiceTest` | 9 | Logique métier avec Mockito (création, modification, suppression, produit introuvable) |
| `ProduitControllerIntegrationTest` | 18 | Contrôleur, service et JPA de bout en bout sur H2 (mode PostgreSQL) : CRUD, filtres, pagination, validation, format d'erreur |
| `OpenApiExportTest` | 1 (ignoré) | Export du contrat `api/openapi.yaml`, activé avec `-Dopenapi.export=true` |

Le projet cible Java 17. Avec un JDK plus récent, utiliser `JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test` (macOS).

## Collections Postman et Bruno

Les deux collections contiennent les mêmes 18 requêtes : 7 pour le parcours des produits (lister, lister par catégorie, créer, consulter, modifier, supprimer, vérifier la suppression), 8 cas d'erreur et 3 requêtes de supervision (santé, métriques, contrat OpenAPI).

- **Postman** : *Import* → `postman/API-Produits.postman_collection.json`, puis clic droit sur la collection → *Run collection*. La variable `baseUrl` vaut `http://localhost:8080` (à modifier si `API_PORT` change). Le dossier *Produits* crée puis supprime un produit : la base retrouve son état initial.
- **Postman en ligne de commande** : `npx newman run postman/API-Produits.postman_collection.json` (nécessite Node.js).
- **Bruno** : voir le README principal.
