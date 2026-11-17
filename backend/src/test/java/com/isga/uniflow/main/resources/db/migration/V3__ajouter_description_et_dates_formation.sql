-- ============================================================
-- V2 : ajout de la description et des dates de début/fin d'une formation
-- (V1 ne doit jamais être modifiée : on ajoute une nouvelle migration)
-- ============================================================

ALTER TABLE formation
    ADD COLUMN description TEXT,
    ADD COLUMN date_debut  DATE,
    ADD COLUMN date_fin    DATE;

-- La date de fin ne peut pas précéder la date de début.
-- (si l'une des deux dates est NULL, la contrainte est ignorée : les anciennes lignes restent valides)
ALTER TABLE formation
    ADD CONSTRAINT ck_formation_dates CHECK (date_fin >= date_debut);