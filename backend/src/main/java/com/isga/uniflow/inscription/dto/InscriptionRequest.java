package com.isga.uniflow.inscription.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corps de la requête d'inscription (POST /api/inscriptions).
 * Une inscription référence exactement un étudiant et une formation.
 * Le statut et la date ne sont jamais envoyés par le front : l'API les fixe.
 */
public record InscriptionRequest(

        @NotNull(message = "L'identifiant de l'étudiant est obligatoire")
        Integer idEtudiant,

        @NotNull(message = "L'identifiant de la formation est obligatoire")
        Integer idFormation
) {
}
