package com.isga.uniflow.etudiant.controller;

import com.isga.uniflow.etudiant.dto.EtudiantRequest;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;
import com.isga.uniflow.etudiant.service.EtudiantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final EtudiantService etudiantService;

    public EtudiantController(EtudiantService etudiantService) {
        this.etudiantService = etudiantService;
    }

    /** crée un étudiant. */
    @PostMapping
    public ResponseEntity<EtudiantResponse> creer(@Valid @RequestBody EtudiantRequest request) {
        EtudiantResponse response = etudiantService.creer(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }
}
