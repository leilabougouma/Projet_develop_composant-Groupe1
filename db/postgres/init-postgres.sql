-- =====================================================================
-- Base CIBLE (PostgreSQL) : produits propres, chargés par l'ETL Apache Hop
-- et utilisés par l'API REST Spring Boot.
-- Exécuté automatiquement au premier démarrage du conteneur PostgreSQL
-- (la base produits_db est créée par la variable POSTGRES_DB).
-- =====================================================================

-- Produits validés et normalisés
CREATE TABLE IF NOT EXISTS produits (
  id            SERIAL PRIMARY KEY,
  nom           VARCHAR(150)  NOT NULL,
  description   TEXT,
  prix          NUMERIC(10,2) NOT NULL CHECK (prix >= 0),
  quantite      INTEGER       NOT NULL DEFAULT 0 CHECK (quantite >= 0),
  categorie     VARCHAR(100)  NOT NULL,
  date_creation TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Lignes refusées par le contrôle qualité de l'ETL
CREATE TABLE IF NOT EXISTS produits_rejets (
  id_source  INT,
  motif      VARCHAR(255),
  date_rejet TIMESTAMP DEFAULT NOW()
);

-- Index pour le filtre par catégorie de l'API (GET /api/v1/produits?categorie=...)
-- et pour la recherche de doublons de l'ETL (clé fonctionnelle nom + categorie)
CREATE INDEX IF NOT EXISTS idx_produits_categorie     ON produits (categorie);
CREATE INDEX IF NOT EXISTS idx_produits_nom_categorie ON produits (nom, categorie);
