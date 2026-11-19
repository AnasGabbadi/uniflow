package com.isga.uniflow.inscription.entity;

import java.util.Arrays;
import java.util.List;

/**
 * Statuts d'une inscription et règles du cycle de vie.
 *
 * <pre>
 *   EN_ATTENTE ──> VALIDEE ──> ANNULEE
 *        └─────────────────────^
 * </pre>
 *
 * <ul>
 *   <li>EN_ATTENTE : demande enregistrée, en attente de validation (statut initial). Occupe une place.</li>
 *   <li>VALIDEE    : inscription confirmée. Occupe une place.</li>
 *   <li>ANNULEE    : inscription annulée. Statut FINAL : n'occupe plus de place et ne peut plus changer.</li>
 * </ul>
 *
 * Transitions autorisées : EN_ATTENTE vers VALIDEE ou ANNULEE ; VALIDEE vers ANNULEE.
 * Toute autre transition (y compris rester dans le même statut) est interdite.
 *
 * Les statuts "actifs" (EN_ATTENTE et VALIDEE) sont ceux qui comptent dans la capacité d'une formation.
 * Aucune transition ne ramène une inscription annulée à un statut actif : la capacité ne peut donc
 * pas être dépassée par un simple changement de statut.
 */
public enum StatutInscription {

    EN_ATTENTE(true),
    VALIDEE(true),
    ANNULEE(false);

    private final boolean actif;

    StatutInscription(boolean actif) {
        this.actif = actif;
    }

    /** Vrai si une inscription dans ce statut occupe une place de la formation. */
    public boolean estActif() {
        return actif;
    }

    /** Vrai si le passage de ce statut vers {@code cible} est autorisé. */
    public boolean peutPasserA(StatutInscription cible) {
        return switch (this) {
            case EN_ATTENTE -> cible == VALIDEE || cible == ANNULEE;
            case VALIDEE -> cible == ANNULEE;
            case ANNULEE -> false;
        };
    }

    /** Noms des statuts actifs, tels qu'enregistrés en base (pour les requêtes de comptage). */
    public static List<String> nomsDesStatutsActifs() {
        return Arrays.stream(values())
                .filter(StatutInscription::estActif)
                .map(Enum::name)
                .toList();
    }
}
