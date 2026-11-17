package com.isga.uniflow.formation.service;

import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.exception.FormationIntrouvableException;
import com.isga.uniflow.formation.repository.FormationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logique métier des formations : création et consultation.
 */
@Service
public class FormationService {

    private final FormationRepository formationRepository;

    public FormationService(FormationRepository formationRepository) {
        this.formationRepository = formationRepository;
    }

    /**
     * Crée une formation ouverte aux inscriptions.
     *
     * @throws FormationDejaExistanteException si l'intitulé est déjà utilisé (HTTP 409)
     */
    @Transactional
    public Formation creer(FormationRequest request) {
        String intitule = request.intitule().trim();

        if (formationRepository.existsByIntituleIgnoreCase(intitule)) {
            throw new FormationDejaExistanteException(intitule);
        }

        Formation formation = new Formation();
        formation.setIntitule(intitule);
        formation.setDescription(request.description());
        formation.setDuree(request.duree());
        formation.setCapaciteMax(request.capaciteMax());
        formation.setDateDebut(request.dateDebut());
        formation.setDateFin(request.dateFin());
        formation.setStatut(Formation.STATUT_OUVERTE);

        return formationRepository.save(formation);
    }

    /**
     * Renvoie la formation correspondant à l'identifiant.
     *
     * @throws FormationIntrouvableException si l'identifiant est inconnu (HTTP 404)
     */
    @Transactional(readOnly = true)
    public Formation trouverParId(Integer id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new FormationIntrouvableException(id));
    }

    /** Renvoie toutes les formations, triées par identifiant (ordre stable). */
    @Transactional(readOnly = true)
    public List<Formation> trouverToutes() {
        return formationRepository.findAll(Sort.by("id"));
    }
}