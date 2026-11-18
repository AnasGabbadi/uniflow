package com.isga.uniflow.etudiant.service;

import com.isga.uniflow.erreur.CodeErreur;
import com.isga.uniflow.erreur.ConflitMetierException;
import com.isga.uniflow.erreur.RessourceIntrouvableException;
import com.isga.uniflow.etudiant.dto.EtudiantRequest;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;


@Service
public class EtudiantService {

    private static final Sort TRI_NOM_PRENOM = Sort.by("nom", "prenom");

    private final EtudiantRepository etudiantRepository;

    public EtudiantService(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    
    @Transactional
    public EtudiantResponse creer(EtudiantRequest request) {
        String email = normaliserEmail(request.email());

        if (etudiantRepository.existsByEmailIgnoreCase(email)) {
            throw emailDejaUtilise(email);
        }

        Etudiant etudiant = new Etudiant();
        appliquer(etudiant, request, email);
        return EtudiantResponse.from(etudiantRepository.save(etudiant));
    }

    
    @Transactional(readOnly = true)
    public EtudiantResponse trouverParId(Integer id) {
        return EtudiantResponse.from(chercher(id));
    }

    
    @Transactional(readOnly = true)
    public List<EtudiantResponse> lister(String recherche) {
        List<Etudiant> etudiants;

        if (recherche == null || recherche.isBlank()) {
            etudiants = etudiantRepository.findAll(TRI_NOM_PRENOM);
        } else {
            String terme = recherche.trim();
            etudiants = etudiantRepository
                    .findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(terme, terme, TRI_NOM_PRENOM);
        }

        return etudiants.stream().map(EtudiantResponse::from).toList();
    }

    
    @Transactional
    public EtudiantResponse modifier(Integer id, EtudiantRequest request) {
        Etudiant etudiant = chercher(id);
        String email = normaliserEmail(request.email());

        if (etudiantRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw emailDejaUtilise(email);
        }

        appliquer(etudiant, request, email);
        return EtudiantResponse.from(etudiantRepository.save(etudiant));
    }


    private Etudiant chercher(Integer id) {
        return etudiantRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("étudiant", id));
    }

    private void appliquer(Etudiant etudiant, EtudiantRequest request, String email) {
        etudiant.setNom(request.nom().trim());
        etudiant.setPrenom(request.prenom().trim());
        etudiant.setEmail(email);
        etudiant.setTelephone(request.telephone());
        etudiant.setAdresse(request.adresse());
        etudiant.setDateNaissance(request.dateNaissance());
    }

    private String normaliserEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ConflitMetierException emailDejaUtilise(String email) {
        return new ConflitMetierException(CodeErreur.EMAIL_DEJA_UTILISE,
                "L'email '" + email + "' est déjà utilisé par un autre étudiant.");
    }
}
