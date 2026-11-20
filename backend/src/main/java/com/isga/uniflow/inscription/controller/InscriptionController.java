package com.isga.uniflow.inscription.controller;

import com.isga.uniflow.inscription.dto.InscriptionRequest;
import com.isga.uniflow.inscription.dto.InscriptionResponse;
import com.isga.uniflow.inscription.dto.StatutInscriptionRequest;
import com.isga.uniflow.inscription.service.InscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Endpoints REST des inscriptions. Aucune logique métier ici : on délègue au service.
 */
@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionController {

    private final InscriptionService inscriptionService;

    public InscriptionController(InscriptionService inscriptionService) {
        this.inscriptionService = inscriptionService;
    }

    /**
     * Inscrit un étudiant à une formation.
     * Réponses : 201 Created (avec l'en-tête Location), 400 si données invalides,
     * 404 si l'étudiant ou la formation n'existe pas, 409 si doublon ou formation complète.
     */
    @PostMapping
    public ResponseEntity<InscriptionResponse> inscrire(@Valid @RequestBody InscriptionRequest request) {
        InscriptionResponse response = inscriptionService.creer(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Liste toutes les inscriptions (liste vide s'il n'y en a aucune).
     */
    @GetMapping
    public ResponseEntity<List<InscriptionResponse>> lister() {
        return ResponseEntity.ok(inscriptionService.lister());
    }

    /**
     * Renvoie une inscription par son identifiant.
     * Réponses : 200 OK, 404 si l'identifiant est inconnu, 400 si l'identifiant n'est pas un nombre.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionResponse> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(inscriptionService.trouverParId(id));
    }

    /**
     * Change le statut d'une inscription. Sert aussi à l'ANNULER : {"statut": "ANNULEE"}.
     * L'annulation libère une place : la capacité d'une formation ne compte que les inscriptions
     * actives (EN_ATTENTE et VALIDEE).
     * Réponses : 200 OK, 400 si statut absent/inconnu, 404 si l'inscription est inconnue,
     * 409 si la transition de statut est interdite.
     */
    @PatchMapping("/{id}/statut")
    public ResponseEntity<InscriptionResponse> changerStatut(@PathVariable Integer id,
                                                             @Valid @RequestBody StatutInscriptionRequest request) {
        return ResponseEntity.ok(inscriptionService.changerStatut(id, request.statut()));
    }
}
