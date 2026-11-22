package com.isga.uniflow.formation.dto;

import com.isga.uniflow.formation.entity.Formation;

import java.time.LocalDate;

/**
 * Représentation d'une formation renvoyée par l'API.
 *
 * {@code placesRestantes} est calculé par l'API (capacité max moins les inscriptions
 * EN_ATTENTE et VALIDEE). Il n'est jamais envoyé par le front et n'est pas stocké en base.
 */
public record FormationResponse(
        Integer id,
        String intitule,
        String description,
        Integer duree,
        Integer capaciteMax,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        Integer placesRestantes
) {

    /** Construit la réponse à partir de l'entité et du nombre de places restantes déjà calculé. */
    public static FormationResponse from(Formation f, int placesRestantes) {
        return new FormationResponse(
                f.getId(),
                f.getIntitule(),
                f.getDescription(),
                f.getDuree(),
                f.getCapaciteMax(),
                f.getDateDebut(),
                f.getDateFin(),
                f.getStatut(),
                placesRestantes);
    }
}