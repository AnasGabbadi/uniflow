package com.isga.uniflow.inscription.dto;

import com.isga.uniflow.inscription.entity.StatutInscription;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête de changement de statut (PATCH /api/inscriptions/{id}/statut).
 * Exemple : {"statut": "ANNULEE"}. Un statut inconnu est rejeté (400) à la lecture du JSON.
 */
public record StatutInscriptionRequest(

        @NotNull(message = "Le statut est obligatoire")
        StatutInscription statut
) {
}
