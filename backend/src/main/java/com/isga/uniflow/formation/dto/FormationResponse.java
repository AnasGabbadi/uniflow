package com.isga.uniflow.formation.dto;

import com.isga.uniflow.formation.entity.Formation;

import java.time.LocalDate;

/**
 * Représentation d'une formation renvoyée par l'API.
 */
public record FormationResponse(
        Integer id,
        String intitule,
        String description,
        Integer duree,
        Integer capaciteMax,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut
) {

    public static FormationResponse from(Formation f) {
        return new FormationResponse(
                f.getId(),
                f.getIntitule(),
                f.getDescription(),
                f.getDuree(),
                f.getCapaciteMax(),
                f.getDateDebut(),
                f.getDateFin(),
                f.getStatut());
    }
}
