package com.isga.uniflow.etudiant.controller;

import com.isga.uniflow.etudiant.dto.EtudiantRequest;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;
import com.isga.uniflow.etudiant.service.EtudiantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final EtudiantService etudiantService;

    public EtudiantController(EtudiantService etudiantService) {
        this.etudiantService = etudiantService;
    }

    @PostMapping
    public ResponseEntity<EtudiantResponse> creer(@Valid @RequestBody EtudiantRequest request) {
        EtudiantResponse response = etudiantService.creer(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EtudiantResponse>> lister(
            @RequestParam(required = false) String recherche) {
        return ResponseEntity.ok(etudiantService.lister(recherche));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EtudiantResponse> trouverParId(@PathVariable Integer id) {
        return ResponseEntity.ok(etudiantService.trouverParId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EtudiantResponse> modifier(@PathVariable Integer id,
                                                     @Valid @RequestBody EtudiantRequest request) {
        return ResponseEntity.ok(etudiantService.modifier(id, request));
    }
}
