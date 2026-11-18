package com.isga.uniflow.etudiant.repository;

import com.isga.uniflow.etudiant.entity.Etudiant;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<Etudiant, Integer> {

    boolean existsByEmailIgnoreCase(String email);

    List<Etudiant> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
            String nom, String prenom, Sort sort);
}
