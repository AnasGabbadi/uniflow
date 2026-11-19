package com.isga.uniflow.inscription.repository;

import com.isga.uniflow.inscription.entity.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Accès aux données des inscriptions (Spring Data JPA).
 */
public interface InscriptionRepository extends JpaRepository<Inscription, Integer> {

    /** Vrai si cet étudiant est déjà inscrit (quel que soit le statut) à cette formation. */
    boolean existsByEtudiant_IdAndFormation_Id(Integer idEtudiant, Integer idFormation);

    /** Nombre d'inscriptions d'une formation dont le statut fait partie de la liste donnée. */
    long countByFormation_IdAndStatutIn(Integer idFormation, Collection<String> statuts);
}
