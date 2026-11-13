package com.isga.uniflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Classe d'association "Inscrit" entre Etudiant et Formation.
 */
@Entity
@Table(
    name = "inscription",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_inscription_etudiant_formation",
        columnNames = {"id_etudiant", "id_formation"})
)
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscription")
    private Integer id;

    @NotNull(message = "L'étudiant est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_etudiant", nullable = false)
    private Etudiant etudiant;

    @NotNull(message = "La formation est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_formation", nullable = false)
    private Formation formation;

    @Column(name = "date_inscription", nullable = false)
    private LocalDate dateInscription = LocalDate.now();

    @NotBlank(message = "Le statut est obligatoire")
    @Pattern(regexp = "EN_ATTENTE|VALIDEE|ANNULEE",
             message = "Statut invalide (EN_ATTENTE, VALIDEE ou ANNULEE)")
    @Column(name = "statut", nullable = false, length = 20)
    private String statut = "EN_ATTENTE";

    public Inscription() {
    }

    public Integer getId() { return id; }

    public Etudiant getEtudiant() { return etudiant; }
    public void setEtudiant(Etudiant etudiant) { this.etudiant = etudiant; }

    public Formation getFormation() { return formation; }
    public void setFormation(Formation formation) { this.formation = formation; }

    public LocalDate getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDate dateInscription) { this.dateInscription = dateInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
