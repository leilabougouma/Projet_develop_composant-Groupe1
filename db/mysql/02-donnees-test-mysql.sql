-- =====================================================================
-- Jeu de données de test (40 lignes) pour la table produits_source
--   * lignes 1 à 30  : produits VALIDES (catégories écrites de plusieurs façons)
--                      dont 1 doublon (ligne 30 = même nom + catégorie que la ligne 7)
--   * lignes 31 à 40 : produits INVALIDES (doivent finir dans produits_rejets)
-- Prix en FCFA.
-- Le détail des résultats attendus est dans db/DONNEES-TEST.md
-- =====================================================================

SET NAMES utf8mb4;
USE produits_source_db;

INSERT INTO produits_source (nom, description, prix, quantite, categorie) VALUES
-- ---------- Electronique (variantes : majuscules, accents, espaces, abréviation)
('Smartphone Galaxy A15',      'Smartphone 128 Go, double SIM',        125000.00,  15, 'Electronique'),
('Téléviseur LED 43 pouces',   'Téléviseur Full HD',                   185000.00,   8, 'ÉLECTRONIQUE'),
('Casque Bluetooth',           'Casque sans fil, autonomie 20 h',       15000.00,  40, ' Electronique '),
('Enceinte portable',          'Enceinte Bluetooth étanche',            22000.00,  25, 'electro'),
('Chargeur rapide USB-C',      'Chargeur 25 W',                          5000.00, 100, 'électronique'),
-- ---------- Informatique
('Ordinateur portable HP 15',  'Core i5, 8 Go RAM, SSD 512 Go',        350000.00,   6, 'Informatique'),
('Souris sans fil',            'Souris optique 2,4 GHz',                 4500.00,  60, 'info'),
('Clavier AZERTY',             'Clavier USB filaire',                    7500.00,  35, 'INFORMATIQUE'),
('Clé USB 64 Go',              'Clé USB 3.0',                            6000.00,  80, 'Info '),
('Imprimante jet d''encre',    'Imprimante couleur multifonction',      65000.00,   5, 'informatique'),
('Disque dur externe 1 To',    'Disque USB 3.0',                        45000.00,  12, ' INFO'),
-- ---------- Vetements
('Pagne Faso Dan Fani',        'Pagne tissé, 2 mètres',                 12000.00,  30, 'Vêtements'),
('Chemise en coton',           'Chemise manches longues',                8000.00,  45, 'vetements'),
('Robe en wax',                'Robe cousue main',                      15000.00,  20, 'habits'),
('Sandales en cuir',           'Sandales artisanales',                   6500.00,  50, 'VETEMENTS'),
('Casquette',                  'Casquette brodée',                       3000.00,  70, ' Vêtements'),
-- ---------- Alimentation
('Riz parfumé 25 kg',          'Sac de riz parfumé',                    18500.00,  40, 'Alimentation'),
('Huile végétale 5 L',         'Bidon d''huile',                         7000.00,  60, 'alimentaire'),
('Sucre en poudre 1 kg',       'Sucre blanc',                             800.00, 200, 'ALIMENTATION'),
('Lait en poudre 400 g',       'Boîte de lait entier',                   2500.00,  90, 'alim'),
('Café moulu 250 g',           'Cas limite : quantité 0 (valide)',       3500.00,   0, 'Alimentation'),
-- ---------- Maison
('Ventilateur sur pied',       'Ventilateur 3 vitesses',                25000.00,  18, 'Maison'),
('Matelas 2 places',           'Matelas mousse 140 x 190',              75000.00,   4, 'maison'),
('Lot d''ustensiles de cuisine','Casseroles et poêles',                 15000.00,  10, 'MAISON '),
('Lampe solaire',              'Lampe rechargeable solaire',             9500.00,  35, ' maison'),
('Moustiquaire imprégnée',     'Moustiquaire 2 places',                  4000.00, 120, 'Maison'),
-- ---------- autres cas limites valides
('Câble HDMI 2 m',             'Cas limite : prix 0 (produit offert)',      0.00,  50, 'electro'),
('Tablette 10 pouces',         'Tablette Android 64 Go',                95000.00,   7, 'Electronique'),
('Sac à dos',                  'Sac à dos 30 L',                        10000.00,  22, 'habits'),
-- ---------- DOUBLON : même nom + même catégorie (après normalisation) que 'Souris sans fil'
('Souris sans fil',            'Doublon : doit mettre à jour, pas créer',  4500.00,  20, 'INFORMATIQUE'),

-- =================== LIGNES INVALIDES (rejets attendus) ===================
(NULL,                         'Rejet : nom absent (NULL)',              5000.00,  10, 'Informatique'),
('',                           'Rejet : nom vide',                       3000.00,  10, 'Maison'),
('   ',                        'Rejet : nom composé uniquement d''espaces', 2000.00, 10, 'Maison'),
('Écouteurs filaires',         'Rejet : prix absent (NULL)',               NULL,   30, 'electro'),
('Montre connectée',           'Rejet : prix négatif',                  -1500.00,   5, 'Electronique'),
('Webcam HD',                  'Rejet : quantité négative',             12000.00,  -3, 'info'),
('Pantalon jean',              'Rejet : catégorie absente (NULL)',      10000.00,  15, NULL),
('Savon de karité',            'Rejet : catégorie vide',                  500.00, 100, ''),
('Ballon de football',         'Catégorie inconnue : rejet OU « Autre » selon décision du groupe', 7000.00, 12, 'jouets'),
(NULL,                         'Rejet : plusieurs erreurs (nom et prix absents)', NULL, 5, 'Maison');
