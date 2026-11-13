package com.isga.uniflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "etudiant")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etudiant")
    private Integer id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Size(max = 30)
    @Column(name = "telephone", length = 30)
    private String telephone;

    @Size(max = 255)
    @Column(name = "adresse")
    private String adresse;

    @Past(message = "La date de naissance doit être dans le passé")
    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    // Un étudiant a plusieurs inscriptions (côté inverse de la relation)
    @JsonIgnore
    @OneToMany(mappedBy = "etudiant", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Inscription> inscriptions = new ArrayList<>();

    public Etudiant() {
    }

    public Integer getId() { return id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public List<Inscription> getInscriptions() { return inscriptions; }
}