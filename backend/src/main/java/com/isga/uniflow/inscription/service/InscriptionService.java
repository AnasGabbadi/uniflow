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
import com.isga.uniflow.inscription.repository.InscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logique métier des inscriptions : inscrire un étudiant à une formation.
 */
@Service
public class InscriptionService {

    /** Statut d'une inscription qui vient d'être créée. (Remplacé par l'enum StatutInscription en branche 2.) */
    private static final String STATUT_INITIAL = "EN_ATTENTE";

    /** Statuts qui occupent une place dans la formation. (Remplacé par l'enum en branche 2.) */
    private static final List<String> STATUTS_ACTIFS = List.of("EN_ATTENTE", "VALIDEE");

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
     * Inscrit un étudiant existant à une formation existante.
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
        long inscriptionsActives =
                inscriptionRepository.countByFormation_IdAndStatutIn(formation.getId(), STATUTS_ACTIFS);

        if (inscriptionsActives >= formation.getCapaciteMax()) {
            throw new ConflitMetierException(CodeErreur.FORMATION_COMPLETE,
                    "La formation '" + formation.getIntitule() + "' est complète (capacité : "
                            + formation.getCapaciteMax() + ").");
        }

        Inscription inscription = new Inscription();
        inscription.setEtudiant(etudiant);
        inscription.setFormation(formation);
        inscription.setStatut(STATUT_INITIAL);

        return InscriptionResponse.from(inscriptionRepository.save(inscription));
    }
}
