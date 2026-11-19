package com.isga.uniflow.erreur;

/** Codes d'erreur stables renvoyés dans le champ {@code code} (voir docs/api-contract.md, §6). */
public enum CodeErreur {
    VALIDATION,
    REQUETE_INVALIDE,
    RESSOURCE_INTROUVABLE,
    EMAIL_DEJA_UTILISE,
    INTITULE_DEJA_UTILISE,
    CAPACITE_INSUFFISANTE,
    INSCRIPTION_EN_DOUBLE,
    FORMATION_COMPLETE,
    FORMATION_NON_OUVERTE,
    TRANSITION_INTERDITE,
    ERREUR_INTERNE,
    TRANSITION_STATUT_INVALIDE
}
