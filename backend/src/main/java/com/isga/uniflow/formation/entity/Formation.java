package com.isga.uniflow.formation.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.isga.uniflow.inscription.entity.Inscription;
@Entity
@Table(name = "formation")
public class Formation {

    public static final String STATUT_OUVERTE = "OUVERTE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_formation")
    private Integer id;

    @Column(name = "intitule", nullable = false, unique = true, length = 150)
    private String intitule;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duree", nullable = false)
    private Integer duree;

    @Column(name = "capacite_max", nullable = false)
    private Integer capaciteMax;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut = STATUT_OUVERTE;

    @JsonIgnore
    @OneToMany(mappedBy = "formation", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Inscription> inscriptions = new ArrayList<>();

    public Formation() {
    }

    public Integer getId() { return id; }

    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }

    public Integer getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(Integer capaciteMax) { this.capaciteMax = capaciteMax; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public List<Inscription> getInscriptions() { return inscriptions; }
}