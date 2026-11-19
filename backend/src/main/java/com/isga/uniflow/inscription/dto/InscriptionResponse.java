package com.isga.uniflow.inscription.dto;

import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.inscription.entity.Inscription;

import java.time.LocalDate;

/**
 * Inscription renvoyée par l'API.
 *
 * L'étudiant et la formation sont exposés sous forme de résumés (identifiant + quelques détails),
 * et non sous forme d'entités : cela évite toute sérialisation JSON récursive
 * (inscription -> étudiant -> inscriptions -> ...).
 */
public record InscriptionResponse(
        Integer id,
        EtudiantResume etudiant,
        FormationResume formation,
        LocalDate dateInscription,
        String statut
) {

    /** Résumé de l'étudiant inscrit. */
    public record EtudiantResume(Integer id, String nom, String prenom, String email) {
    }

    /** Résumé de la formation. */
    public record FormationResume(Integer id, String intitule) {
    }

    public static InscriptionResponse from(Inscription inscription) {
        Etudiant e = inscription.getEtudiant();
        Formation f = inscription.getFormation();

        return new InscriptionResponse(
                inscription.getId(),
                new EtudiantResume(e.getId(), e.getNom(), e.getPrenom(), e.getEmail()),
                new FormationResume(f.getId(), f.getIntitule()),
                inscription.getDateInscription(),
                inscription.getStatut());
    }
}
