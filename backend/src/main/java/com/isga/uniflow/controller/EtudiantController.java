
package com.isga.uniflow.controller;

import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.repository.EtudiantRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/etudiants")
public class EtudiantController {

    private final EtudiantRepository repository;

    public EtudiantController(EtudiantRepository repository) {
        this.repository = repository;
    }

    // GET : afficher tous les étudiants ou rechercher
    @GetMapping
    public List<Etudiant> lister(
            @RequestParam(required = false) String recherche) {

        if (recherche == null || recherche.isBlank()) {
            return repository.findAll();
        }

        String terme = recherche.trim();

        return repository
                .findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
                        terme, terme
                );
    }

    // GET : afficher un étudiant par son ID
    @GetMapping("/{id}")
    public Etudiant obtenir(@PathVariable Integer id) {
        return trouverEtudiant(id);
    }

    // POST : ajouter un étudiant
    @PostMapping
    public ResponseEntity<Etudiant> creer(
            @Valid @RequestBody Etudiant etudiant) {

        etudiant.setNom(etudiant.getNom().trim());
        etudiant.setPrenom(etudiant.getPrenom().trim());
        etudiant.setEmail(etudiant.getEmail().trim());

        Etudiant nouveau = repository.save(etudiant);

        return ResponseEntity.status(HttpStatus.CREATED).body(nouveau);
    }

    // PUT : modifier un étudiant
    @PutMapping("/{id}")
    public Etudiant modifier(
            @PathVariable Integer id,
            @Valid @RequestBody Etudiant donnees) {

        Etudiant etudiant = trouverEtudiant(id);

        etudiant.setNom(donnees.getNom().trim());
        etudiant.setPrenom(donnees.getPrenom().trim());
        etudiant.setEmail(donnees.getEmail().trim());
        etudiant.setTelephone(donnees.getTelephone());
        etudiant.setAdresse(donnees.getAdresse());
        etudiant.setDateNaissance(donnees.getDateNaissance());

        return repository.save(etudiant);
    }

    // DELETE : supprimer un étudiant
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {

        Etudiant etudiant = trouverEtudiant(id);

        repository.delete(etudiant);

        return ResponseEntity.noContent().build();
    }

    private Etudiant trouverEtudiant(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Étudiant introuvable : " + id
                ));
    }
}
