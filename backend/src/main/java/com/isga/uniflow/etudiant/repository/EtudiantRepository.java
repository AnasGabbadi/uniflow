package com.isga.uniflow.etudiant.repository;

import com.isga.uniflow.etudiant.entity.Etudiant;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    /**
     * IP-39 : recherche partielle, insensible à la casse, sur le nom, le prénom
     * ou le nom complet (« prénom nom » ou « nom prénom »).
     */
    @Query("""
            SELECT e FROM Etudiant e
            WHERE LOWER(e.nom) LIKE LOWER(CONCAT('%', :terme, '%'))
               OR LOWER(e.prenom) LIKE LOWER(CONCAT('%', :terme, '%'))
               OR LOWER(CONCAT(e.prenom, ' ', e.nom)) LIKE LOWER(CONCAT('%', :terme, '%'))
               OR LOWER(CONCAT(e.nom, ' ', e.prenom)) LIKE LOWER(CONCAT('%', :terme, '%'))
            """)
    List<Etudiant> rechercherParNom(@Param("terme") String terme, Sort sort);
}
