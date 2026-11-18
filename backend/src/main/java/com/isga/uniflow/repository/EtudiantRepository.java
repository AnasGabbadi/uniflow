
package com.isga.uniflow.repository;

import com.isga.uniflow.etudiant.entity.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Integer> {

    List<Etudiant> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
            String nom,
            String prenom
    );
}
