# Partie 2 : Extraction, transformation et chargement des données (ETL)

## 1. Objectif

Cette partie a pour objectif de transférer les produits de la base source MySQL vers la base cible PostgreSQL, utilisée par l'API REST. Les données sources sont volontairement imparfaites : valeurs manquantes, prix ou quantités négatifs, catégories écrites de plusieurs façons et doublons. L'ETL doit donc contrôler chaque ligne, normaliser les catégories et ne charger que des données fiables.

## 2. Choix de l'outil

L'énoncé proposait Talend Open Studio. Cet outil n'étant plus maintenu depuis 2024, nous avons retenu **Apache Hop** (version 2.18.1). Ce choix repose sur trois raisons :

- c'est un projet libre et actif de la fondation Apache, issu de Pentaho Kettle ;
- ses traitements sont enregistrés dans des fichiers XML, que l'on peut versionner avec Git ;
- il fournit des images Docker officielles, ce qui rend l'exécution reproductible sans installation manuelle.

## 3. Démarche

### 3.1 Organisation du projet Hop

Nous avons créé un projet Hop nommé `produits-etl`. Il contient :

- deux **connexions** : `mysql_source` pour la source et `postgres_cible` pour la cible. Elles sont définies uniquement par des variables, afin qu'aucun mot de passe n'apparaisse dans les fichiers versionnés ;
- un **environnement** `docker`, qui fournit les adresses des bases. Les identifiants sont lus dans le fichier `.env` au démarrage du conteneur ;
- un **référentiel** `categories.csv`, qui associe chaque variante de catégorie à sa catégorie officielle.

### 3.2 Le pipeline de traitement (`pl_produits`)

Le pipeline traite les données en cinq étapes :

1. **Extraction** : lecture de la table `produits_source` dans MySQL.
2. **Normalisation** : suppression des espaces superflus dans le nom ; mise en minuscules de la catégorie et suppression des espaces et des accents (par exemple, « ÉLECTRONIQUE » devient « electronique »).
3. **Correspondance** : recherche de la catégorie officielle dans le référentiel (« electro » devient « Electronique »).
4. **Contrôle** : vérification des règles de gestion (nom, prix et catégorie obligatoires, prix et quantité positifs ou nuls, catégorie connue). Pour chaque ligne invalide, un motif précis est produit, par exemple « prix négatif ».
5. **Chargement** : les lignes valides sont insérées dans la table `produits`, ou mises à jour si un produit de même nom et de même catégorie existe déjà, ce qui évite les doublons. Les lignes invalides sont enregistrées dans la table `produits_rejets`, avec leur motif.

### 3.3 Le workflow d'orchestration (`wf_etl_produits`)

Le workflow enchaîne les opérations dans l'ordre suivant : vérification des connexions, exécution du pipeline, calcul d'un bilan (pipeline `pl_bilan`), puis journalisation du résultat. En cas d'erreur, il écrit un message d'échec et se termine avec un code d'erreur, ce qui permet de l'intégrer au test de bout en bout.

### 3.4 Exécution avec Docker

Deux services ont été ajoutés au fichier `docker-compose.yml` :

- `etl` exécute le workflow en une commande (`docker compose run --rm etl`), puis s'arrête ;
- `hop-web` offre l'interface graphique de Hop dans un navigateur, sur le port 8090. Ce port a été choisi pour éviter tout conflit avec l'API, qui utilise le port 8080.

## 4. Résultats

Le jeu de test contient 40 produits. L'exécution de l'ETL donne les résultats suivants :

| Indicateur | Valeur |
|---|---|
| Lignes lues dans MySQL | 40 |
| Lignes valides | 30 |
| Produits chargés dans PostgreSQL | 29 |
| Lignes rejetées | 10 |

L'écart entre les 30 lignes valides et les 29 produits chargés s'explique par un doublon : la ligne 30 reprend le produit « Souris sans fil » et met à jour la ligne existante au lieu d'en créer une nouvelle. La catégorie inconnue « jouets » a été rejetée, conformément à la décision du groupe. Ces résultats correspondent exactement aux résultats attendus décrits dans `db/DONNEES-TEST.md`. Une seconde exécution donne les mêmes chiffres, ce qui montre que l'ETL peut être relancé sans créer de doublons.

## 5. Difficultés rencontrées

- **Pilote MySQL** : Apache Hop ne fournit pas le pilote MySQL, pour des raisons de licence. Il est donc ajouté automatiquement lors de la construction de l'image Docker.
- **Accents dans les catégories** : aucun composant standard de Hop ne supprime les accents. Cette opération a été réalisée par un court script JavaScript, intégré au pipeline.
- **Protection des mots de passe** : l'image Docker officielle affichait les mots de passe dans le journal. Nous les transmettons donc par un fichier temporaire, créé au démarrage du conteneur.

## 6. Conclusion

L'ETL réalisé avec Apache Hop répond aux exigences de l'énoncé : il extrait les données de MySQL, applique les règles de qualité, normalise les catégories et charge dans PostgreSQL des données propres et sans doublon. Son exécution entièrement conteneurisée le rend reproductible sur n'importe quelle machine disposant de Docker. Il fournit ainsi à l'API et aux tests une base de données cohérente.
