package com.isga.uniflow.formation.controller;

import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.formation.dto.FormationResponse;
import com.isga.uniflow.formation.service.FormationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Endpoints REST des formations. Aucune logique métier ici : on délègue au service.
 */
@RestController
@RequestMapping("/api/formations")
public class FormationController {

    private final FormationService formationService;

    public FormationController(FormationService formationService) {
        this.formationService = formationService;
    }

    /**
     * Crée une formation. Renvoie 201 Created avec l'en-tête Location.
     */
    @PostMapping
    public ResponseEntity<FormationResponse> creer(@Valid @RequestBody FormationRequest request) {
        FormationResponse response = formationService.creer(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Modifie une formation existante.
     * Réponses : 200 OK, 400 si données invalides, 404 si l'identifiant est inconnu,
     * 409 si l'intitulé est déjà pris ou si la capacité est inférieure aux inscriptions actives.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FormationResponse> modifier(@PathVariable Integer id,
                                                      @Valid @RequestBody FormationRequest request) {
        return ResponseEntity.ok(formationService.modifier(id, request));
    }

    /**
     * Renvoie une formation par son identifiant.
     * Réponses : 200 OK, 404 si l'identifiant est inconnu, 400 si l'identifiant n'est pas un nombre.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FormationResponse> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(formationService.trouverParId(id));
    }

    /**
     * Renvoie la liste de toutes les formations (liste vide s'il n'y en a aucune).
     */
    @GetMapping
    public ResponseEntity<List<FormationResponse>> trouverToutes() {
        return ResponseEntity.ok(formationService.trouverToutes());
    }

    /**
     * Supprime une formation qui n'a aucune inscription.
     * Réponses : 204 No Content, 404 si l'identifiant est inconnu,
     * 409 si la formation a des inscriptions, 400 si l'identifiant n'est pas un nombre.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        formationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
