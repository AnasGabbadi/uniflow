package com.isga.uniflow.formation.service;

import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.etudiant.dto.EtudiantRequest;

import com.isga.uniflow.formation.dto.FormationResponse;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;

import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.etudiant.entity.Etudiant;

import com.isga.uniflow.formation.exception.FormationAvecInscriptionsException;
import com.isga.uniflow.formation.exception.FormationCapaciteInsuffisanteException;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.exception.FormationIntrouvableException;

import com.isga.uniflow.formation.repository.FormationRepository;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logique métier des formations : création, consultation, et conversion entité
 * -> DTO.
 * Le nombre de places restantes est calculé ici, à partir des inscriptions
 * actives.
 */
@Service
public class FormationService {

    private final FormationRepository formationRepository;

    public FormationService(FormationRepository formationRepository) {
        this.formationRepository = formationRepository;
    }

    /**
     * Crée une formation. Le statut vaut OUVERTE si le client n'en fournit pas.
     *
     * @throws FormationDejaExistanteException si l'intitulé est déjà utilisé (HTTP
     *                                         409)
     */
    @Transactional
    public FormationResponse creer(FormationRequest request) {
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
        formation.setStatut(request.statut() != null ? request.statut() : Formation.STATUT_OUVERTE);

        Formation enregistree = formationRepository.save(formation);

        // Une formation qui vient d'être créée n'a aucune inscription : toutes les
        // places sont libres.
        return FormationResponse.from(enregistree, placesRestantes(enregistree, 0));
    }

    /**
     * Renvoie la formation correspondant à l'identifiant, avec ses places
     * restantes.
     *
     * @throws FormationIntrouvableException si l'identifiant est inconnu (HTTP 404)
     */
    @Transactional(readOnly = true)
    public FormationResponse trouverParId(Integer id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new FormationIntrouvableException(id));

        long actives = formationRepository.compterInscriptionsActives(id);
        return FormationResponse.from(formation, placesRestantes(formation, actives));
    }

    /**
     * renvoie les étudiants inscrits à une formation (inscription EN_ATTENTE ou
     * VALIDEE),
     * triés par nom puis prénom. Les inscriptions annulées sont exclues.
     *
     * @throws FormationIntrouvableException si l'identifiant est inconnu (HTTP 404)
     */
    @Transactional(readOnly = true)
    public List<EtudiantResponse> trouverEtudiants(Integer id) {
        if (!formationRepository.existsById(id)) {
            throw new FormationIntrouvableException(id);
        }

        return formationRepository.trouverEtudiantsActifs(id).stream()
                .map(EtudiantResponse::from)
                .toList();
    }

    /**
     * Renvoie toutes les formations (triées par identifiant), avec leurs places
     * restantes.
     */
    @Transactional(readOnly = true)
    public List<FormationResponse> trouverToutes() {
        List<Formation> formations = formationRepository.findAll(Sort.by("id"));

        // Une seule requête pour toutes les formations : Map id_formation ->
        // inscriptions actives
        Map<Integer, Long> activesParFormation = new HashMap<>();
        for (Object[] ligne : formationRepository.compterInscriptionsActivesParFormation()) {
            activesParFormation.put(((Number) ligne[0]).intValue(), ((Number) ligne[1]).longValue());
        }

        return formations.stream()
                .map(f -> FormationResponse.from(f,
                        placesRestantes(f, activesParFormation.getOrDefault(f.getId(), 0L))))
                .toList();
    }

    /**
     * Modifie une formation existante.
     * Si le statut n'est pas fourni, le statut actuel est conservé.
     *
     * @throws FormationIntrouvableException          si l'identifiant est inconnu
     *                                                (HTTP 404)
     * @throws FormationDejaExistanteException        si l'intitulé appartient à une
     *                                                autre formation (HTTP 409)
     * @throws FormationCapaciteInsuffisanteException si la capacité est inférieure
     *                                                aux inscriptions actives (HTTP
     *                                                409)
     */
    @Transactional
    public FormationResponse modifier(Integer id, FormationRequest request) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new FormationIntrouvableException(id));

        String intitule = request.intitule().trim();
        if (formationRepository.existsByIntituleIgnoreCaseAndIdNot(intitule, id)) {
            throw new FormationDejaExistanteException(intitule);
        }

        // On ne peut pas retirer des places déjà occupées par des inscriptions actives
        long actives = formationRepository.compterInscriptionsActives(id);
        if (request.capaciteMax() < actives) {
            throw new FormationCapaciteInsuffisanteException(request.capaciteMax(), actives);
        }

        formation.setIntitule(intitule);
        formation.setDescription(request.description());
        formation.setDuree(request.duree());
        formation.setCapaciteMax(request.capaciteMax());
        formation.setDateDebut(request.dateDebut());
        formation.setDateFin(request.dateFin());
        if (request.statut() != null) {
            formation.setStatut(request.statut());
        }

        Formation enregistree = formationRepository.save(formation);
        return FormationResponse.from(enregistree, placesRestantes(enregistree, actives));
    }

    /**
     * Supprime une formation.
     * Politique : la suppression est refusée dès que la formation a au moins une
     * inscription, quel que
     * soit son statut (les inscriptions annulées sont de l'historique et la clé
     * étrangère les protège).
     * On ne supprime donc que les formations sans aucune inscription.
     *
     * @throws FormationIntrouvableException      si l'identifiant est inconnu (HTTP
     *                                            404)
     * @throws FormationAvecInscriptionsException si la formation a des inscriptions
     *                                            (HTTP 409)
     */
    @Transactional
    public void supprimer(Integer id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new FormationIntrouvableException(id));

        long inscriptions = formationRepository.compterToutesInscriptions(id);
        if (inscriptions > 0) {
            throw new FormationAvecInscriptionsException(id, inscriptions);
        }

        formationRepository.delete(formation);
    }

    /**
     * Places restantes = capacité max - inscriptions actives (EN_ATTENTE et
     * VALIDEE).
     * Jamais négatif, même si les données sont incohérentes.
     */
    private int placesRestantes(Formation formation, long inscriptionsActives) {
        return (int) Math.max(0, formation.getCapaciteMax() - inscriptionsActives);
    }
}
