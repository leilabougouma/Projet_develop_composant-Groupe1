# Jeu de données de test (MySQL → `produits_source`)

Le fichier `db/mysql/02-donnees-test-mysql.sql` insère **40 produits** dans la base source.
Ce document sert de référence au **Membre 2 (ETL)** pour construire le référentiel des catégories
et au **Membre 5 (tests)** pour vérifier les résultats.

## 1. Répartition des lignes

| id source | Contenu | Résultat attendu après l'ETL |
|---|---|---|
| 1 – 29 | Produits valides, catégories écrites de plusieurs façons | Chargés dans `produits` |
| 30 | Doublon de la ligne 7 (`Souris sans fil` + Informatique) | **Mise à jour** de la ligne existante, pas de nouvelle ligne |
| 31 – 38, 40 | Valeurs obligatoires manquantes ou invalides | Dans `produits_rejets` avec un motif |
| 39 | Catégorie inconnue (`jouets`) | Rejet **ou** catégorie « Autre » (décision du groupe) |

## 2. Lignes invalides (rejets attendus)

| id | Problème |
|---|---|
| 31 | `nom` NULL |
| 32 | `nom` vide (`''`) |
| 33 | `nom` composé uniquement d'espaces (`'   '`) : il faut faire un *trim* avant le contrôle |
| 34 | `prix` NULL |
| 35 | `prix` négatif (-1500) |
| 36 | `quantite` négative (-3) |
| 37 | `categorie` NULL |
| 38 | `categorie` vide (`''`) |
| 39 | `categorie` inconnue (`jouets`) |
| 40 | Plusieurs erreurs (`nom` et `prix` NULL) |

Cas limites **valides** à ne pas rejeter : `quantite = 0` (id 21) et `prix = 0` (id 27).

## 3. Variantes de catégories présentes dans la source

Après *trim* + minuscules (+ suppression des accents), chaque variante doit correspondre à une catégorie officielle :

| Variantes dans MySQL | Catégorie officielle |
|---|---|
| `Electronique`, `ÉLECTRONIQUE`, ` Electronique `, `electro`, `électronique` | **Electronique** |
| `Informatique`, `info`, `INFORMATIQUE`, `Info `, ` INFO`, `informatique` | **Informatique** |
| `Vêtements`, `vetements`, `habits`, `VETEMENTS`, ` Vêtements` | **Vetements** |
| `Alimentation`, `alimentaire`, `ALIMENTATION`, `alim` | **Alimentation** |
| `Maison`, `maison`, `MAISON `, ` maison` | **Maison** |

Proposition de référentiel `etl/referentiel/categories.csv` (variantes déjà en minuscules, sans accents ni espaces) :

```csv
variante,categorie_officielle
electronique,Electronique
electro,Electronique
informatique,Informatique
info,Informatique
vetements,Vetements
habits,Vetements
alimentation,Alimentation
alimentaire,Alimentation
alim,Alimentation
maison,Maison
```

## 4. Résultat attendu de l'ETL

| Option choisie pour `jouets` | Lignes lues | Lignes valides | Lignes dans `produits` | Lignes dans `produits_rejets` |
|---|---|---|---|---|
| Catégorie inconnue **rejetée** | 40 | 30 | **29** (30 − 1 doublon) | **10** |
| Catégorie inconnue → **« Autre »** | 40 | 31 | **30** | **9** |

Répartition dans `produits` (option « rejet ») : Electronique 7, Informatique 6, Vetements 6, Alimentation 5, Maison 5.
