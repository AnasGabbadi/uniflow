package com.isga.uniflow.inscription.repository;

import com.isga.uniflow.inscription.entity.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Accès aux données des inscriptions (Spring Data JPA).
 */
public interface InscriptionRepository extends JpaRepository<Inscription, Integer> {

    /** Vrai si cet étudiant est déjà inscrit (quel que soit le statut) à cette formation. */
    boolean existsByEtudiant_IdAndFormation_Id(Integer idEtudiant, Integer idFormation);

    /** Nombre d'inscriptions d'une formation dont le statut fait partie de la liste donnée. */
    long countByFormation_IdAndStatutIn(Integer idFormation, Collection<String> statuts);

    /**
     * Toutes les inscriptions, triées par identifiant, avec leur étudiant et leur formation
     * chargés dans la même requête (JOIN FETCH) : évite une requête supplémentaire par inscription.
     */
    @Query("""
            SELECT i FROM Inscription i
            JOIN FETCH i.etudiant
            JOIN FETCH i.formation
            ORDER BY i.id
            """)
    List<Inscription> findAllAvecDetails();

    /** Une inscription par identifiant, avec son étudiant et sa formation chargés. */
    @Query("""
            SELECT i FROM Inscription i
            JOIN FETCH i.etudiant
            JOIN FETCH i.formation
            WHERE i.id = :id
            """)
    Optional<Inscription> findByIdAvecDetails(@Param("id") Integer id);
}
