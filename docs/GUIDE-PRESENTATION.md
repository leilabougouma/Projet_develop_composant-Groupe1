# Guide pour préparer la présentation PowerPoint

Ce guide dit **quoi mettre dans chaque diapositive**, **qui parle**, **combien de temps**, et comment réussir la **démonstration**. Il s'appuie sur le rapport (`rapport/rapport.pdf`) : les chiffres et les schémas sont les mêmes, pour que l'oral et l'écrit se répondent.

> **Hypothèses à confirmer.** Aucune consigne de soutenance n'était disponible. Le plan suppose **15 minutes de présentation + 5 minutes de questions** et **5 intervenants** (un par partie). Si la durée ou le nombre d'intervenants change, ajuster le minutage de la section 2 (garder la même logique : 1 diapo ≈ 1 minute).

---

## 1. Le fil rouge : une histoire en 4 temps

Un jury retient une histoire, pas une liste de technologies. Racontez :

1. **Le problème** : des données de produits sales (prix négatifs, catégories écrites de dix façons, doublons) ne sont pas exploitables.
2. **La solution** : une chaîne qui nettoie, stocke, expose et surveille ces données.
3. **La preuve** : une démonstration en direct et des chiffres vérifiés.
4. **Les leçons** : ce qui a été difficile, ce que nous ferions ensuite.

Phrase d'ouverture suggérée : *« Nous avons 40 lignes de produits, dont 10 sont inutilisables. Voici comment notre chaîne les détecte, charge les 29 bonnes, et les expose dans une API supervisée. »*

### Les chiffres à connaître par cœur

| Chiffre | Signification |
|---|---|
| **40 / 30 / 29 / 10** | Lignes lues / valides / chargées / rejetées par l'ETL (30 valides mais 29 chargées : un doublon met à jour une ligne existante) |
| **5** catégories officielles | Electronique, Informatique, Vetements, Alimentation, Maison |
| **6** services Docker | mysql, postgres, etl, hop-web, api, prometheus |
| **5** opérations CRUD + **3** endpoints techniques | liste, consultation, création, modification, suppression ; santé, métriques, contrat |
| **27** tests automatisés | 9 unitaires + 18 d'intégration, sans Docker |
| **18** requêtes Bruno | 7 produits, 8 erreurs, 3 supervision |

---

## 2. Plan des diapositives (15 minutes)

Règle : **une idée par diapositive**, un titre qui énonce le message (pas « Introduction », mais « Des données brutes inutilisables »).

| N° | Titre de la diapo (message) | Contenu | Visuel | Durée | Qui |
|---|---|---|---|---|---|
| 1 | **Titre** | Nom du projet, module, groupe, membres, date | Logo de l'école si disponible | 0:15 | Tous |
| 2 | **Des données brutes inutilisables** | 3 exemples concrets de lignes sales : prix `-1500`, catégories `electro` / `ÉLECTRONIQUE`, doublon | Extrait du tableau `produits_source` (3 lignes surlignées) | 0:45 | Membre 1 |
| 3 | **Notre solution en un schéma** | La chaîne MySQL → ETL → PostgreSQL → API, et Prometheus qui surveille | **Schéma d'architecture** du rapport (fig. 1) | 1:00 | Membre 1 |
| 4 | **Tout se lance avec une commande** | `docker compose up -d` ; 6 services ; `.env` pour les secrets | Capture de `docker compose ps` | 0:45 | Membre 1 |
| 5 | **L'ETL : contrôler avant de charger** | Les 5 étapes (extraction, normalisation, correspondance, contrôle, chargement) ; rejets avec motif | **Schéma du pipeline** du rapport (fig. 2) | 1:15 | Membre 2 |
| 6 | **Résultat : 40 lignes → 29 produits + 10 rejets** | Tableau des chiffres ; idempotence (relancer ne crée pas de doublon) ; pourquoi Apache Hop et pas Talend | Tableau 40/30/29/10 + capture Hop Web | 1:15 | Membre 2 |
| 7 | **L'API : une architecture en couches** | Controller → Service → Repository ; DTO ; erreurs centralisées | **Schéma des couches** du rapport (fig. 3) | 1:15 | Membre 3 |
| 8 | **Des erreurs toujours au même format** | Exemple de réponse 400 avec `details` ; 404 ; route inconnue | Bloc JSON d'erreur (gros caractères) | 0:45 | Membre 3 |
| 9 | **Un contrat OpenAPI versionné** | Swagger UI ; `openapi.yaml` dans Git ; DELETE enrichi (200 + message) | Capture de Swagger UI | 0:45 | Membre 3 |
| 10 | **Supervision : voir l'API vivre** | Prometheus interroge `/actuator/prometheus` toutes les 5 s ; 3 requêtes PromQL utiles | Capture « Targets » (UP) + un graphique | 1:15 | Membre 4 |
| 11 | **Tester à trois niveaux** | 27 tests auto, collection Bruno (18 requêtes), scénario de bout en bout | Capture du Runner Bruno (tout en vert) | 1:15 | Membre 5 |
| 12 | **DÉMONSTRATION** | Voir section 3 | Écran partagé, pas de diapo chargée | 3:00 | Membre 5 |
| 13 | **Difficultés et leçons** | 3 difficultés réelles : Talend abandonné, conflits de ports, Lombok / Java 27 | 3 icônes + 1 phrase chacune | 0:30 | Au choix |
| 14 | **Limites et perspectives** | Sécurité (JWT), ETL planifié, Grafana + alertes, Testcontainers | 4 puces courtes | 0:30 | Au choix |
| 15 | **Conclusion + questions** | Rappel du fil rouge en 2 phrases ; « Merci, des questions ? » | Le schéma d'architecture, en petit | 0:15 | Tous |

Total : environ **14 min 45 s**, soit une marge d'une quinzaine de secondes sur 15 minutes. Si le temps presse, fusionner les diapos 8 et 9, ou condenser 13 et 14 en une seule.

### Notes orales (ce qu'il faut dire, diapo par diapo)

- **Diapo 2.** Montrez, ne décrivez pas : « Ici, un prix négatif. Ici, la même catégorie écrite de trois façons. Une base comme celle-ci ne peut pas alimenter une application. »
- **Diapo 3.** Suivez le schéma avec le doigt ou le pointeur, de gauche à droite : « Les données entrent par MySQL, l'ETL les nettoie, PostgreSQL les garde propres, l'API les expose, Prometheus la surveille. » Insistez : **tout est dans Docker**.
- **Diapo 4.** Un seul message : *n'importe qui peut relancer le projet*. Citez le `.env` : *aucun mot de passe dans le dépôt*.
- **Diapo 5.** Prenez **un exemple de bout en bout** : « `ÉLECTRONIQUE` devient `electronique`, puis `Electronique` grâce au référentiel. »
- **Diapo 6.** Expliquez l'écart 30 → 29 : *« la ligne 30 est un doublon : elle met à jour "Souris sans fil" au lieu d'en créer une deuxième. »* Justifiez Hop en une phrase : *Talend Open Studio n'est plus maintenu depuis 2024, l'énoncé autorise une alternative.*
- **Diapo 7.** « Chaque couche n'appelle que la suivante. Le contrôleur ne connaît pas la base. » Mentionnez les DTO : *l'entité n'est jamais exposée directement.*
- **Diapo 8.** « Toutes les erreurs, même une URL inconnue, ont le même format : le client n'a qu'un cas à gérer. »
- **Diapo 9.** Mentionnez que le DELETE renvoie maintenant le produit supprimé avec un message : c'est une décision de conception assumée (changement de contrat 204 → 200, tout a été mis à jour).
- **Diapo 10.** Montrez que Prometheus n'est pas magique : il **va chercher** les métriques ; si l'API tombe, `up` passe à 0.
- **Diapo 11.** La pyramide : beaucoup de tests rapides (unitaires), moins de tests d'intégration, quelques tests de bout en bout.

---

## 3. La démonstration (3 minutes)

La démo est le moment le plus marquant et le plus risqué. **Répétez-la au moins 3 fois**, chronomètre en main.

### Avant de passer sur scène (à faire 15 minutes avant)

```bash
cd Projet_develop_composant-Groupe1
docker compose down -v                 # état propre
docker compose up -d --build           # démarre tout
docker compose ps                      # mysql et postgres "healthy"
curl http://localhost:8080/actuator/health    # "UP"
```

- Ouvrir à l'avance, dans des onglets : **Swagger UI** (`:8080/swagger-ui/index.html`), **Prometheus** (`:9090/targets`), **Bruno** (collection ouverte, environnement *Local* sélectionné).
- Si le port 5432 est pris sur la machine de présentation : mettre `PG_PORT=5434` dans `.env`.
- Fermer les notifications, passer en mode « Ne pas déranger », agrandir la police du terminal (≥ 18 pt).

### Le déroulé (3 minutes)

| Temps | Action | Ce que le jury voit | Ce que vous dites |
|---|---|---|---|
| 0:00 | `bash db/verifier-bases.sh` | 40 lignes en source, tables cibles vides | « Au départ : 40 lignes sales, une cible vide. » |
| 0:30 | `docker compose run --rm etl` | Journal : 40 lues, 29 chargées, 10 rejetées | « L'ETL nettoie et charge. » |
| 1:15 | Swagger UI → `GET /api/v1/produits?categorie=informatique` | Liste filtrée, casse ignorée | « L'API sert les données propres. » |
| 1:45 | Bruno → Runner sur le dossier **Produits** | Création, lecture, modification, suppression, 404, tout en vert | « Le scénario complet, automatisé. » |
| 2:30 | Prometheus → Targets, puis une requête | `produits-api` UP, compteurs en hausse | « Et chaque requête que vous venez de voir est mesurée. » |

> **Durée de l'ETL (mesurée le 28/10/2026) :** le workflow s'exécute en **≈ 1,5 s**, mais la commande `docker compose run --rm etl` prend **≈ 18 s** (démarrage du conteneur Hop) quand l'image existe déjà, et **≈ 1 min 25 s la toute première fois** (construction de l'image). **Lancez-la une fois avant la soutenance pour que l'image soit construite**, puis refaites `docker compose down -v && docker compose up -d --build` pour repartir d'une base vide : en direct, comptez alors ≈ 18 s, ce qui est acceptable à l'oral (parlez pendant l'attente).

### Plan B (obligatoire)

Une démo en direct peut échouer (réseau, Docker, port occupé). Préparez :

1. **Une vidéo de 2 à 3 minutes** de la démo réussie (enregistrement d'écran).
2. **Des captures d'écran** de chaque étape, dans une diapo de secours.
3. Une phrase de transition prête : *« Le temps est précieux, je vous montre l'enregistrement de la même démonstration. »* Ne vous excusez pas plus de dix secondes.

---

## 4. Captures d'écran à préparer

Les mêmes images servent à la présentation **et au rapport** : déposez-les dans `rapport/figures/` avec **exactement** ces noms, elles apparaîtront automatiquement dans le PDF après `cd rapport && tectonic rapport.tex`.

| Fichier | À capturer | Où dans le rapport |
|---|---|---|
| `hop-pipeline.png` | Le pipeline `pl_produits` dans Hop Web (`:8090/ui`) | Partie 2 |
| `swagger-ui.png` | La page Swagger UI avec les 5 opérations dépliées | Partie 3 |
| `prometheus-targets.png` | Prometheus → Status → Targets (2 cibles UP) | Partie 4 |
| `prometheus-requetes.png` | Une requête PromQL avec son graphique | Partie 4 |
| `bruno-runner.png` | Le Runner Bruno après exécution, tout vert | Partie 5 |

Pour la présentation uniquement : `docker compose ps`, le journal de l'ETL (40 / 29 / 10) et `/actuator/health` (détail des composants).

**Conseils de capture :** fenêtre propre (pas d'autres onglets), zoom du navigateur à 125 %, recadrage serré, format PNG.

---

## 5. Conception visuelle

- **Palette** : celle du rapport, volontairement sobre : texte et titres en gris anthracite `#404040`, fonds en gris clair `#F2F2F2`, fond de diapo blanc. Pas de rouge ni de vert vifs ; pour souligner un élément, utilisez le **gras** ou un cadre fin gris, pas une couleur criarde.
- **Texte** : titres 32–40 pt, corps **≥ 24 pt**, **3 puces maximum**, 6 mots par puce environ. Si une diapo demande de petits caractères, c'est qu'elle contient trop de choses : coupez-la en deux.
- **Code et JSON** : police à chasse fixe, ≥ 20 pt, **5 lignes maximum**, la ligne importante surlignée.
- **Tableaux** : comme dans le rapport, avec **bordures sur toutes les lignes et colonnes**, une ligne d'en-tête en gris clair `#F2F2F2` (texte noir, gras) et un texte ≥ 20 pt. Dans PowerPoint : *Insertion > Tableau*, puis *Création de tableau > Bordures > Toutes les bordures*. Pas plus de 5 lignes et 4 colonnes par diapo.
- **Schémas** : reprenez ceux du rapport (architecture, ETL, couches), redessinés en grand. Un schéma vaut mieux que dix puces.
- **Cohérence** : même modèle de diapositive, mêmes noms que dans le rapport (`produits_source`, `produits_rejets`…).
- **Numérotation** : numéro de diapo en bas à droite, pour que le jury puisse y renvoyer ses questions.
- **À éviter** : phrases complètes sur les diapos, animations décoratives, captures illisibles, logos de technologies en vrac.

---

## 6. Répartition de la parole

| Intervenant | Diapos | Durée approximative |
|---|---|---|
| Membre 1 (bases, Docker) | 2 à 4 | 2 min 30 |
| Membre 2 (ETL) | 5 à 6 | 2 min 30 |
| Membre 3 (API) | 7 à 9 | 2 min 45 |
| Membre 4 (supervision) | 10 | 1 min 15 |
| Membre 5 (tests, démo) | 11 à 12 | 4 min 15 |
| Titre et conclusion | 1, 13 à 15 | 1 min 30 (répartie) |

Chacun prépare **ses** diapos et connaît les **voisines** : le jury peut poser une question sur une autre partie. Prévoyez des **transitions** d'une phrase entre intervenants (*« Maintenant que les données sont propres, [nom] vous explique comment elles sont exposées. »*).

---

## 7. Questions probables du jury et éléments de réponse

| Question | Réponse en deux phrases |
|---|---|
| **Pourquoi Apache Hop et pas Talend ?** | Talend Open Studio n'est plus maintenu depuis 2024 et son installation n'est plus fiable. L'énoncé autorise une alternative ; Hop est un projet Apache actif, ses fichiers sont en XML (donc dans Git) et il a une image Docker officielle. |
| **Que se passe-t-il si on relance l'ETL deux fois ?** | Rien de plus : le chargement est un *insert ou update* sur la clé (nom, catégorie) et la table des rejets est vidée à chaque passage. On obtient toujours 29 produits. |
| **Pourquoi deux bases ?** | Cela reproduit un cas réel : la source est non fiable (aucune contrainte), la cible impose les règles (`NOT NULL`, `CHECK`). Chaque couche se protège. |
| **Pourquoi des DTO ?** | Pour ne pas exposer l'entité JPA : le contrat de l'API peut évoluer indépendamment du schéma de la base, et la validation est portée par le DTO d'entrée. |
| **Pourquoi `ddl-auto: validate` ?** | Le schéma est écrit en SQL et partagé par l'ETL, l'API et les tests. Hibernate ne le modifie jamais ; si le code et la table divergent, l'API refuse de démarrer, ce qui évite les erreurs silencieuses. |
| **Pourquoi le DELETE renvoie-t-il 200 et pas 204 ?** | Nous avons voulu une réponse plus informative : un message de confirmation et le produit supprimé. C'est un changement de contrat assumé ; tests, contrat OpenAPI, README et Bruno ont été mis à jour. Les deux choix sont valides en REST. |
| **L'API est-elle sécurisée ?** | Non, c'est une limite assumée pour un environnement de développement. En production : Spring Security avec JWT, et accès restreint à `/actuator` (l'état de santé détaillé est public ici). |
| **Comment Prometheus sait-il où chercher ?** | Via `monitoring/prometheus.yml` : une cible `api:8080` et un chemin `/actuator/prometheus`, collectés toutes les 5 s. Si l'API est arrêtée, la métrique `up` passe à 0. |
| **Pourquoi H2 pour les tests et pas PostgreSQL ?** | H2 en mode PostgreSQL permet des tests rapides, sans Docker, avec le même schéma. Une amélioration serait Testcontainers pour tester sur un vrai PostgreSQL. |
| **Qu'est-ce qui était le plus difficile ?** | Choisir franchement trois exemples : l'abandon de Talend, la suppression des accents (aucun composant Hop ne le fait, d'où un script JavaScript), les conflits de ports sur nos machines. |
| **Comment relancer le projet depuis zéro ?** | `cp .env.example .env`, puis `docker compose up -d --build` et `docker compose run --rm etl`. Trois commandes, documentées dans le README. |
| **Que feriez-vous avec plus de temps ?** | Alertes Prometheus (Alertmanager) et tableaux Grafana, ETL planifié, authentification JWT, intégration continue qui rejoue le scénario de bout en bout. |

**Règle d'or :** si vous ne savez pas, dites-le : *« Nous n'avons pas testé ce cas ; voici comment nous nous y prendrions. »* C'est mieux qu'une invention.

---

## 8. Checklist

### La veille
- [ ] Le rapport est compilé (`cd rapport && tectonic rapport.tex`) et les champs en rouge sont remplis : noms des membres, enseignant, répartition, résultats du scénario de bout en bout.
- [ ] Les 5 captures sont dans `rapport/figures/` et le rapport a été recompilé.
- [ ] La présentation a été répétée en entier, **chronométrée**, avec la démo.
- [ ] La vidéo de secours est enregistrée et ouvre sur l'ordinateur de présentation.
- [ ] Le PowerPoint est exporté en **PDF** (copie de secours) et copié sur une clé USB.

### Le jour même
- [ ] Ordinateur branché sur secteur, notifications coupées, écran en mode « étendu » testé avec le projecteur.
- [ ] `docker compose up -d --build` lancé **15 minutes avant** ; tous les services sont `healthy`.
- [ ] Onglets (Swagger, Prometheus, Bruno) et terminal ouverts, police agrandie.
- [ ] Une bouteille d'eau, et l'ordre de passage connu de tous.

### Les erreurs classiques
- Lire les diapos à voix haute : le jury lit plus vite que vous.
- Passer 5 minutes sur la technologie et 1 minute sur la démonstration : inversez.
- Tourner le dos au public pour regarder l'écran : utilisez l'écran de l'ordinateur.
- Enchaîner sans transition entre les intervenants.
- Découvrir la démo pendant la soutenance : elle doit être répétée.
