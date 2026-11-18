package com.isga.uniflow.etudiant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public record EtudiantRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 255, message = "L'email ne doit pas dépasser 255 caractères")
        String email,

        @Size(max = 30, message = "Le téléphone ne doit pas dépasser 30 caractères")
        @Pattern(regexp = "[0-9 +\\-]*",
                 message = "Le téléphone ne peut contenir que des chiffres, des espaces, + et -")
        String telephone,

        @Size(max = 255, message = "L'adresse ne doit pas dépasser 255 caractères")
        String adresse,

        @Past(message = "La date de naissance doit être dans le passé")
        LocalDate dateNaissance
) {
}
