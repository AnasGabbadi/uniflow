package com.isga.uniflow.etudiant.service;

import com.isga.uniflow.erreur.CodeErreur;
import com.isga.uniflow.erreur.ConflitMetierException;
import com.isga.uniflow.etudiant.dto.EtudiantRequest;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;


@Service
public class EtudiantService {

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
