package com.isga.uniflow.formation.service;



import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.repository.FormationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormationService {

    private final FormationRepository formationRepository;

    public FormationService(FormationRepository formationRepository) {
        this.formationRepository = formationRepository;
    }

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
}