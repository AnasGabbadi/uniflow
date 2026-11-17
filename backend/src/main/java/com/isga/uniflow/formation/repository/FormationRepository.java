package com.isga.uniflow.formation.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.isga.uniflow.formation.entity.Formation;

public interface FormationRepository extends JpaRepository<Formation, Integer> {

    boolean existsByIntituleIgnoreCase(String intitule);
}
