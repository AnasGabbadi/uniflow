-- ============================================================
-- V1 : création du schéma initial (Etudiant, Formation, Inscription)
-- ============================================================

CREATE TABLE etudiant (
    id_etudiant     SERIAL       PRIMARY KEY,
    nom             VARCHAR(100) NOT NULL,
    prenom          VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    telephone       VARCHAR(30),
    adresse         VARCHAR(255),
    date_naissance  DATE,
    CONSTRAINT uq_etudiant_email UNIQUE (email)
);

CREATE TABLE formation (
    id_formation    SERIAL       PRIMARY KEY,
    intitule        VARCHAR(150) NOT NULL,
    duree           INTEGER      NOT NULL,
    capacite_max    INTEGER      NOT NULL,
    statut          VARCHAR(20)  NOT NULL DEFAULT 'OUVERTE',
    CONSTRAINT uq_formation_intitule UNIQUE (intitule),
    CONSTRAINT ck_formation_duree     CHECK (duree > 0),
    CONSTRAINT ck_formation_capacite  CHECK (capacite_max > 0),
    CONSTRAINT ck_formation_statut    CHECK (statut IN ('OUVERTE', 'FERMEE', 'ARCHIVEE'))
);

-- Classe d'association "Inscrit" entre Etudiant et Formation (relation N-N)
CREATE TABLE inscription (
    id_inscription    SERIAL      PRIMARY KEY,
    id_etudiant       INTEGER     NOT NULL,
    id_formation      INTEGER     NOT NULL,
    date_inscription  DATE        NOT NULL DEFAULT CURRENT_DATE,
    statut            VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    CONSTRAINT fk_inscription_etudiant
        FOREIGN KEY (id_etudiant)  REFERENCES etudiant (id_etudiant)  ON DELETE CASCADE,
    CONSTRAINT fk_inscription_formation
        FOREIGN KEY (id_formation) REFERENCES formation (id_formation) ON DELETE CASCADE,
    -- un étudiant ne peut pas s'inscrire deux fois à la même formation
    CONSTRAINT uq_inscription_etudiant_formation UNIQUE (id_etudiant, id_formation),
    CONSTRAINT ck_inscription_statut CHECK (statut IN ('EN_ATTENTE', 'VALIDEE', 'ANNULEE'))
);