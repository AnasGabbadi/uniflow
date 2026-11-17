package com.isga.uniflow.formation.controller;

import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.formation.dto.FormationResponse;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.service.FormationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
        Formation formation = formationService.creer(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(formation.getId())
                .toUri();

        return ResponseEntity.created(location).body(FormationResponse.from(formation));
    }

    /**
     * Renvoie une formation par son identifiant.
     * Réponses : 200 OK, 404 si l'identifiant est inconnu, 400 si l'identifiant n'est pas un nombre.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FormationResponse> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(FormationResponse.from(formationService.trouverParId(id)));
    }

    /**
     * Renvoie la liste de toutes les formations (liste vide s'il n'y en a aucune).
     */
    @GetMapping
    public ResponseEntity<List<FormationResponse>> trouverToutes() {
        List<FormationResponse> formations = formationService.trouverToutes().stream()
                .map(FormationResponse::from)
                .toList();
        return ResponseEntity.ok(formations);
    }
}