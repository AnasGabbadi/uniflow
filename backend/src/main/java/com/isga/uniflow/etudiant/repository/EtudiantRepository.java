package com.isga.uniflow.etudiant.repository;

import com.isga.uniflow.etudiant.entity.Etudiant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<Etudiant, Integer> {

    boolean existsByEmailIgnoreCase(String email);
}
