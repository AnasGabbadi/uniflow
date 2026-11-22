package com.isga.uniflow.etudiant.dto;

import com.isga.uniflow.etudiant.entity.Etudiant;

import java.time.LocalDate;

public record EtudiantResponse(
        Integer id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        LocalDate dateNaissance
) {

    public static EtudiantResponse from(Etudiant e) {
        return new EtudiantResponse(
                e.getId(),
                e.getNom(),
                e.getPrenom(),
                e.getEmail(),
                e.getTelephone(),
                e.getAdresse(),
                e.getDateNaissance());
    }
}
