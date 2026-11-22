package com.isga.uniflow.formation.repository;

import com.isga.uniflow.formation.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Accès aux données des formations (Spring Data JPA).
 *
 * Les deux requêtes de comptage sont en SQL natif : elles lisent la table inscription
 * sans que le module formation dépende de la classe Inscription du module inscription.
 * Elles supposent les colonnes inscription.id_formation et inscription.statut (migration V3).
 * Inscriptions "actives" = statut EN_ATTENTE ou VALIDEE (celles qui occupent une place).
 */
public interface FormationRepository extends JpaRepository<Formation, Integer> {

    /** Vrai si une formation porte déjà cet intitulé (comparaison insensible à la casse). */
    boolean existsByIntituleIgnoreCase(String intitule);

    /**
     * Vrai si une AUTRE formation (identifiant différent) porte déjà cet intitulé.
     * Sert à la modification : une formation peut garder son propre intitulé.
     */
    boolean existsByIntituleIgnoreCaseAndIdNot(String intitule, Integer id);

    /** Nombre d'inscriptions actives pour une formation. */
    @Query(value = """
            SELECT COUNT(*) FROM inscription
            WHERE id_formation = :idFormation AND statut IN ('EN_ATTENTE', 'VALIDEE')
            """, nativeQuery = true)
    long compterInscriptionsActives(@Param("idFormation") Integer idFormation);

    /** Nombre total d'inscriptions d'une formation, tous statuts confondus (sert à la suppression). */
    @Query(value = "SELECT COUNT(*) FROM inscription WHERE id_formation = :idFormation", nativeQuery = true)
    long compterToutesInscriptions(@Param("idFormation") Integer idFormation);

    /**
     * Nombre d'inscriptions actives pour chaque formation, en une seule requête (évite une requête
     * par formation dans la liste). Chaque ligne : [id_formation, nombre]. Les formations sans
     * inscription active n'apparaissent pas.
     */
    @Query(value = """
            SELECT id_formation, COUNT(*) FROM inscription
            WHERE statut IN ('EN_ATTENTE', 'VALIDEE')
            GROUP BY id_formation
            """, nativeQuery = true)
    List<Object[]> compterInscriptionsActivesParFormation();
}
