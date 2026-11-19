package com.isga.uniflow.inscription.service;

import com.isga.uniflow.erreur.CodeErreur;
import com.isga.uniflow.erreur.ConflitMetierException;
import com.isga.uniflow.erreur.RessourceIntrouvableException;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.repository.FormationRepository;
import com.isga.uniflow.inscription.dto.InscriptionRequest;
import com.isga.uniflow.inscription.dto.InscriptionResponse;
import com.isga.uniflow.inscription.entity.Inscription;
import com.isga.uniflow.inscription.entity.StatutInscription;
import com.isga.uniflow.inscription.repository.InscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logique métier des inscriptions : inscrire un étudiant, consulter les inscriptions,
 * et faire évoluer le statut d'une inscription.
 */
@Service
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormationRepository formationRepository;

    public InscriptionService(InscriptionRepository inscriptionRepository,
                              EtudiantRepository etudiantRepository,
                              FormationRepository formationRepository) {
        this.inscriptionRepository = inscriptionRepository;
        this.etudiantRepository = etudiantRepository;
        this.formationRepository = formationRepository;
    }

    /**
     * Inscrit un étudiant existant à une formation existante (statut initial : EN_ATTENTE).
     * Ordre des vérifications : étudiant, formation, doublon, capacité.
     *
     * @throws RessourceIntrouvableException si l'étudiant ou la formation n'existe pas (HTTP 404)
     * @throws ConflitMetierException        si l'étudiant est déjà inscrit ou si la formation est complète (HTTP 409)
     */
    @Transactional
    public InscriptionResponse creer(InscriptionRequest request) {
        Etudiant etudiant = etudiantRepository.findById(request.idEtudiant())
                .orElseThrow(() -> new RessourceIntrouvableException("étudiant", request.idEtudiant()));

        Formation formation = formationRepository.findById(request.idFormation())
                .orElseThrow(() -> new RessourceIntrouvableException("formation", request.idFormation()));

        if (inscriptionRepository.existsByEtudiant_IdAndFormation_Id(etudiant.getId(), formation.getId())) {
            throw new ConflitMetierException(CodeErreur.INSCRIPTION_EN_DOUBLE,
                    "L'étudiant " + etudiant.getPrenom() + " " + etudiant.getNom()
                            + " est déjà inscrit à la formation '" + formation.getIntitule() + "'.");
        }

        // La capacité ne tient compte que des inscriptions actives (les annulées libèrent leur place)
        long inscriptionsActives = inscriptionRepository.countByFormation_IdAndStatutIn(
                formation.getId(), StatutInscription.nomsDesStatutsActifs());

        if (inscriptionsActives >= formation.getCapaciteMax()) {
            throw new ConflitMetierException(CodeErreur.FORMATION_COMPLETE,
                    "La formation '" + formation.getIntitule() + "' est complète (capacité : "
                            + formation.getCapaciteMax() + ").");
        }

        Inscription inscription = new Inscription();
        inscription.setEtudiant(etudiant);
        inscription.setFormation(formation);
        inscription.setStatut(StatutInscription.EN_ATTENTE.name());

        return InscriptionResponse.from(inscriptionRepository.save(inscription));
    }

    /** Renvoie toutes les inscriptions (triées par identifiant), liste vide s'il n'y en a aucune. */
    @Transactional(readOnly = true)
    public List<InscriptionResponse> lister() {
        return inscriptionRepository.findAllAvecDetails().stream()
                .map(InscriptionResponse::from)
                .toList();
    }

    /**
     * Renvoie une inscription par son identifiant.
     *
     * @throws RessourceIntrouvableException si l'inscription n'existe pas (HTTP 404)
     */
    @Transactional(readOnly = true)
    public InscriptionResponse trouverParId(Integer id) {
        Inscription inscription = inscriptionRepository.findByIdAvecDetails(id)
                .orElseThrow(() -> new RessourceIntrouvableException("inscription", id));
        return InscriptionResponse.from(inscription);
    }

    /**
     * Fait passer une inscription au statut demandé, si la transition est autorisée
     * (voir {@link StatutInscription}).
     *
     * @throws RessourceIntrouvableException si l'inscription n'existe pas (HTTP 404)
     * @throws ConflitMetierException        si la transition est interdite (HTTP 409)
     */
    @Transactional
    public InscriptionResponse changerStatut(Integer id, StatutInscription cible) {
        Inscription inscription = inscriptionRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("inscription", id));

        StatutInscription actuel = StatutInscription.valueOf(inscription.getStatut());

        if (!actuel.peutPasserA(cible)) {
            throw new ConflitMetierException(CodeErreur.TRANSITION_INTERDITE,
                    "Transition de statut interdite : " + actuel + " vers " + cible + ".");
        }

        // Aucune vérification de capacité ici : aucune transition ne ramène une inscription annulée
        // à un statut actif, donc le nombre de places occupées ne peut pas augmenter.
        inscription.setStatut(cible.name());
        return InscriptionResponse.from(inscriptionRepository.save(inscription));
    }
}
