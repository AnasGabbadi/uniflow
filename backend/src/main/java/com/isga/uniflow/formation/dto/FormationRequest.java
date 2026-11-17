package com.isga.uniflow.formation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Corps de la requête de création d'une formation.
 */
public record FormationRequest(

        @NotBlank(message = "L'intitulé est obligatoire")
        @Size(max = 150, message = "L'intitulé ne doit pas dépasser 150 caractères")
        String intitule,

        @Size(max = 5000, message = "La description ne doit pas dépasser 5000 caractères")
        String description,

        @NotNull(message = "La durée est obligatoire")
        @Positive(message = "La durée doit être strictement positive")
        Integer duree,

        @NotNull(message = "La capacité est obligatoire")
        @Positive(message = "La capacité doit être strictement positive")
        Integer capaciteMax,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin
) {

    /**
     * La date de fin ne peut pas précéder la date de début.
     * Si l'une des dates est absente, @NotNull signale déjà l'erreur : on ne la double pas.
     */
    @JsonIgnore
    @AssertTrue(message = "La date de fin ne peut pas précéder la date de début")
    public boolean isPeriodeValide() {
        return dateDebut == null || dateFin == null || !dateFin.isBefore(dateDebut);
    }
}