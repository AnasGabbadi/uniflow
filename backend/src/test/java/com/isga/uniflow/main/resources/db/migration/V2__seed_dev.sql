-- V2__seed_dev.sql : données de développement (UniFlow)
-- Chemin : src/main/resources/db/migration/V2__seed_dev.sql
-- Ajustez les noms de tables/colonnes à vos entités. Si la table utilisateur
-- existe déjà dans V1, supprimez le CREATE TABLE ci-dessous.

CREATE TABLE IF NOT EXISTS utilisateur (
  id_utilisateur SERIAL PRIMARY KEY,
  email          VARCHAR(255) NOT NULL UNIQUE,
  mot_de_passe   VARCHAR(100) NOT NULL,
  role           VARCHAR(30)  NOT NULL
);

-- Compte admin de DEV uniquement : admin@uniflow.local / admin123 (hash BCrypt)
INSERT INTO utilisateur (email, mot_de_passe, role) VALUES
  ('admin@uniflow.local', '$2a$10$G6L6dCQhH5LIA0dozySyFegO58t/VB1O35KY9BAn7x31zYrkHZLly', 'ADMIN')
ON CONFLICT (email) DO NOTHING;

INSERT INTO formation (id_formation, intitule, duree, capacite_max, statut) VALUES
  (1, 'Génie Logiciel',        24, 30, 'OUVERTE'),
  (2, 'Data Science',          18, 25, 'OUVERTE'),
  (3, 'Cybersécurité',         12, 20, 'ARCHIVEE'),
  (4, 'Réseaux et Télécoms',   24, 30, 'FERMEE');

INSERT INTO etudiant (id_etudiant, nom, prenom, email, telephone, adresse, date_naissance) VALUES
  (1, 'Benali',  'Yassine', 'yassine.benali@mail.com', '0600000001', 'Casablanca', '2002-03-14'),
  (2, 'Haddad',  'Nour',    'nour.haddad@mail.com',    '0600000002', 'Rabat',      '2001-11-02'),
  (3, 'Alaoui',  'Rania',   'rania.alaoui@mail.com',   '0600000003', 'Fès',        '2003-06-21'),
  (4, 'Tazi',    'Omar',    'omar.tazi@mail.com',      '0600000004', 'Marrakech',  '2002-09-30'),
  (5, 'Berrada', 'Salma',   'salma.berrada@mail.com',  '0600000005', 'Tanger',     '2000-01-18');

INSERT INTO inscription (id_inscription, id_etudiant, id_formation, date_inscription, statut) VALUES
  (1, 1, 1, '2026-09-01', 'VALIDEE'),
  (2, 2, 1, '2026-09-02', 'VALIDEE'),
  (3, 2, 2, '2026-09-03', 'EN_ATTENTE'),
  (4, 3, 2, '2026-09-03', 'VALIDEE'),
  (5, 4, 3, '2026-09-05', 'ANNULEE'),
  (6, 5, 1, '2026-09-06', 'VALIDEE');

-- Si les ids sont auto-générés (SERIAL / IDENTITY), réalignez les séquences :
SELECT setval(pg_get_serial_sequence('formation',   'id_formation'),   (SELECT MAX(id_formation)   FROM formation));
SELECT setval(pg_get_serial_sequence('etudiant',    'id_etudiant'),    (SELECT MAX(id_etudiant)    FROM etudiant));
SELECT setval(pg_get_serial_sequence('inscription', 'id_inscription'), (SELECT MAX(id_inscription) FROM inscription));
