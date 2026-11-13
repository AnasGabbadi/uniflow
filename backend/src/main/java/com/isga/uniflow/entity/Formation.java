package com.isga.uniflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "formation")
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_formation")
    private Integer id;

    @NotBlank(message = "L'intitulé est obligatoire")
    @Size(max = 150)
    @Column(name = "intitule", nullable = false, unique = true, length = 150)
    private String intitule;

    @NotNull(message = "La durée est obligatoire")
    @Positive(message = "La durée doit être positive")
    @Column(name = "duree", nullable = false)
    private Integer duree;

    @NotNull(message = "La capacité maximale est obligatoire")
    @Positive(message = "La capacité maximale doit être positive")
    @Column(name = "capacite_max", nullable = false)
    private Integer capaciteMax;

    @NotBlank(message = "Le statut est obligatoire")
    @Pattern(regexp = "OUVERTE|FERMEE|ARCHIVEE",
             message = "Statut invalide (OUVERTE, FERMEE ou ARCHIVEE)")
    @Column(name = "statut", nullable = false, length = 20)
    private String statut = "OUVERTE";

    @JsonIgnore
    @OneToMany(mappedBy = "formation", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Inscription> inscriptions = new ArrayList<>();

    public Formation() {
    }

    public Integer getId() { return id; }

    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }

    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }

    public Integer getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(Integer capaciteMax) { this.capaciteMax = capaciteMax; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public List<Inscription> getInscriptions() { return inscriptions; }
}