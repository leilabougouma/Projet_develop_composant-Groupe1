-- =====================================================================
-- Base SOURCE (MySQL) : produits "bruts", avant passage dans l'ETL
-- Exécuté automatiquement au premier démarrage du conteneur MySQL
-- (dossier monté dans /docker-entrypoint-initdb.d/)
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS produits_source_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE produits_source_db;

DROP TABLE IF EXISTS produits_source;

-- Volontairement SANS contraintes NOT NULL / CHECK :
-- la source contient des données sales, c'est l'ETL qui fait le contrôle qualité.
CREATE TABLE produits_source (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  nom           VARCHAR(150),
  description   TEXT,
  prix          DECIMAL(10,2),
  quantite      INT,
  categorie     VARCHAR(100),   -- ex : 'electro', 'ÉLECTRONIQUE', ' Electronique '
  date_creation DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
