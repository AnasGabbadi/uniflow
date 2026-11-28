package com.isga.uniflow.formation.service;

import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.etudiant.dto.EtudiantRequest;

import com.isga.uniflow.formation.dto.FormationResponse;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;

import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.etudiant.entity.Etudiant;

import com.isga.uniflow.formation.exception.FormationAvecInscriptionsException;
import com.isga.uniflow.formation.exception.FormationCapaciteInsuffisanteException;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.exception.FormationIntrouvableException;
import com.isga.uniflow.formation.repository.FormationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste FormationService avec un repository simulé (Mockito) : pas de base de
 * données.
 */
@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock
    private FormationRepository repository;

    @InjectMocks
    private FormationService service;

    private FormationRequest requete(String statut) {
        return new FormationRequest("  Java avancé ", "Spring Boot", 40, 20,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20), statut);
    }

    // ---------- Création ----------

    @Test
    void creerEnregistreLaFormationAvecLesDonneesFournies() {
        when(repository.existsByIntituleIgnoreCase("Java avancé")).thenReturn(false);
        when(repository.save(any(Formation.class))).thenAnswer(inv -> inv.getArgument(0));

        FormationResponse response = service.creer(requete("FERMEE"));

        ArgumentCaptor<Formation> captor = ArgumentCaptor.forClass(Formation.class);
        verify(repository).save(captor.capture());
        Formation saved = captor.getValue();

        assertEquals("Java avancé", saved.getIntitule()); // intitulé nettoyé (trim)
        assertEquals("Spring Boot", saved.getDescription());
        assertEquals(40, saved.getDuree());
        assertEquals(20, saved.getCapaciteMax());
        assertEquals(LocalDate.of(2026, 12, 1), saved.getDateDebut());
        assertEquals(LocalDate.of(2026, 12, 20), saved.getDateFin());
        assertEquals("FERMEE", saved.getStatut()); // statut fourni respecté

        // La réponse reflète ce qui a été enregistré ; aucune inscription : toutes les
        // places sont libres
        assertEquals("Java avancé", response.intitule());
        assertEquals("FERMEE", response.statut());
        assertEquals(20, response.placesRestantes());
    }

    @Test
    void creerUtiliseLeStatutOuverteParDefaut() {
        when(repository.existsByIntituleIgnoreCase("Java avancé")).thenReturn(false);
        when(repository.save(any(Formation.class))).thenAnswer(inv -> inv.getArgument(0));

        FormationResponse response = service.creer(requete(null));

        assertEquals("OUVERTE", response.statut());
    }

    @Test
    void creerRefuseUnIntituleDejaUtilise() {
        when(repository.existsByIntituleIgnoreCase("Java avancé")).thenReturn(true);

        assertThrows(FormationDejaExistanteException.class, () -> service.creer(requete(null)));
        verify(repository, never()).save(any());
    }

    // ---------- Consultation par identifiant ----------

    @Test
    void trouverParIdCalculeLesPlacesRestantes() {
        Formation formation = uneFormation(1, "Java avancé", 20);
        when(repository.findById(1)).thenReturn(Optional.of(formation));
        when(repository.compterInscriptionsActives(1)).thenReturn(7L);

        FormationResponse response = service.trouverParId(1);

        assertEquals(1, response.id());
        assertEquals("Java avancé", response.intitule());
        assertEquals("Spring Boot", response.description());
        assertEquals(LocalDate.of(2026, 12, 1), response.dateDebut());
        assertEquals(LocalDate.of(2026, 12, 20), response.dateFin());
        assertEquals(13, response.placesRestantes()); // 20 - 7
    }

    @Test
    void trouverParIdNeDepassePasZeroPlaceRestante() {
        Formation formation = uneFormation(1, "Java avancé", 5);
        when(repository.findById(1)).thenReturn(Optional.of(formation));
        when(repository.compterInscriptionsActives(1)).thenReturn(8L); // données incohérentes

        assertEquals(0, service.trouverParId(1).placesRestantes());
    }

    @Test
    void trouverParIdLeveUneExceptionSiLIdentifiantEstInconnu() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        FormationIntrouvableException ex = assertThrows(FormationIntrouvableException.class,
                () -> service.trouverParId(99));

        assertEquals("Aucune formation avec l'identifiant 99", ex.getMessage());
    }

    // ---------- Consultation de la liste ----------

    @Test
    void trouverToutesCalculeLesPlacesRestantesDeChaqueFormation() {
        Formation f1 = uneFormation(1, "Java avancé", 20);
        Formation f2 = uneFormation(2, "Docker et Kubernetes", 25);
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(f1, f2));
        // Seule la formation 1 a des inscriptions actives (4)
        when(repository.compterInscriptionsActivesParFormation())
                .thenReturn(Collections.singletonList(new Object[] { 1, 4L }));

        List<FormationResponse> resultat = service.trouverToutes();

        assertEquals(2, resultat.size());
        assertEquals("Java avancé", resultat.get(0).intitule());
        assertEquals(16, resultat.get(0).placesRestantes()); // 20 - 4
        assertEquals("Docker et Kubernetes", resultat.get(1).intitule());
        assertEquals(25, resultat.get(1).placesRestantes()); // aucune inscription : 25 libres
    }

    @Test
    void trouverToutesRenvoieUneListeVideQuandIlNYADeFormation() {
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of());
        when(repository.compterInscriptionsActivesParFormation()).thenReturn(Collections.emptyList());

        assertTrue(service.trouverToutes().isEmpty());
    }

    // ---------- Modification ----------

    @Test
    void modifierMetAJourLesChampsEtCalculeLesPlacesRestantes() {
        Formation formation = uneFormation(1, "Ancien titre", 20);
        when(repository.findById(1)).thenReturn(Optional.of(formation));
        when(repository.existsByIntituleIgnoreCaseAndIdNot("Java avancé", 1)).thenReturn(false);
        when(repository.compterInscriptionsActives(1)).thenReturn(6L);
        when(repository.save(any(Formation.class))).thenAnswer(inv -> inv.getArgument(0));

        FormationResponse response = service.modifier(1, requete("FERMEE"));

        assertEquals(1, response.id());
        assertEquals("Java avancé", response.intitule()); // intitulé nettoyé (trim)
        assertEquals("Spring Boot", response.description());
        assertEquals(40, response.duree());
        assertEquals(20, response.capaciteMax());
        assertEquals(LocalDate.of(2026, 12, 1), response.dateDebut());
        assertEquals(LocalDate.of(2026, 12, 20), response.dateFin());
        assertEquals("FERMEE", response.statut());
        assertEquals(14, response.placesRestantes()); // 20 - 6
    }

    @Test
    void modifierConserveLeStatutActuelSiAucunStatutN_EstFourni() {
        Formation formation = uneFormation(1, "Ancien titre", 20);
        formation.setStatut("ARCHIVEE");
        when(repository.findById(1)).thenReturn(Optional.of(formation));
        when(repository.existsByIntituleIgnoreCaseAndIdNot("Java avancé", 1)).thenReturn(false);
        when(repository.compterInscriptionsActives(1)).thenReturn(0L);
        when(repository.save(any(Formation.class))).thenAnswer(inv -> inv.getArgument(0));

        FormationResponse response = service.modifier(1, requete(null));

        assertEquals("ARCHIVEE", response.statut());
    }

    @Test
    void modifierAccepteUneCapaciteEgaleAuxInscriptionsActives() {
        Formation formation = uneFormation(1, "Java avancé", 30);
        when(repository.findById(1)).thenReturn(Optional.of(formation));
        when(repository.existsByIntituleIgnoreCaseAndIdNot("Java avancé", 1)).thenReturn(false);
        when(repository.compterInscriptionsActives(1)).thenReturn(20L); // capacité demandée : 20
        when(repository.save(any(Formation.class))).thenAnswer(inv -> inv.getArgument(0));

        FormationResponse response = service.modifier(1, requete(null));

        assertEquals(20, response.capaciteMax());
        assertEquals(0, response.placesRestantes());
    }

    @Test
    void modifierRefuseUneCapaciteInferieureAuxInscriptionsActives() {
        Formation formation = uneFormation(1, "Java avancé", 30);
        when(repository.findById(1)).thenReturn(Optional.of(formation));
        when(repository.existsByIntituleIgnoreCaseAndIdNot("Java avancé", 1)).thenReturn(false);
        when(repository.compterInscriptionsActives(1)).thenReturn(21L); // capacité demandée : 20

        FormationCapaciteInsuffisanteException ex = assertThrows(
                FormationCapaciteInsuffisanteException.class, () -> service.modifier(1, requete(null)));

        assertEquals("La capacité (20) ne peut pas être inférieure au nombre d'inscriptions actives (21)",
                ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void modifierRefuseUnIntituleDejaUtiliseParUneAutreFormation() {
        when(repository.findById(1)).thenReturn(Optional.of(uneFormation(1, "Ancien titre", 20)));
        when(repository.existsByIntituleIgnoreCaseAndIdNot("Java avancé", 1)).thenReturn(true);

        assertThrows(FormationDejaExistanteException.class, () -> service.modifier(1, requete(null)));
        verify(repository, never()).save(any());
    }

    @Test
    void modifierLeveUneExceptionSiLIdentifiantEstInconnu() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(FormationIntrouvableException.class, () -> service.modifier(99, requete(null)));
        verify(repository, never()).save(any());
    }

    // ---------- Utilitaire ----------

    /**
     * Formation réelle avec un id fixé (l'entité n'a pas de setId : on passe par
     * réflexion).
     */
    private Formation uneFormation(int id, String intitule, int capaciteMax) {
        Formation f = new Formation();
        ReflectionTestUtils.setField(f, "id", id);
        f.setIntitule(intitule);
        f.setDescription("Spring Boot");
        f.setDuree(40);
        f.setCapaciteMax(capaciteMax);
        f.setDateDebut(LocalDate.of(2026, 12, 1));
        f.setDateFin(LocalDate.of(2026, 12, 20));
        f.setStatut(Formation.STATUT_OUVERTE);
        return f;
    }

    // ---------- Suppression ----------

    @Test
    void supprimerEffaceLaFormationSansInscription() {
        Formation formation = new Formation();
        ReflectionTestUtils.setField(formation, "id", 5);
        when(repository.findById(5)).thenReturn(Optional.of(formation));
        when(repository.compterToutesInscriptions(5)).thenReturn(0L);

        service.supprimer(5);

        verify(repository).delete(formation);
    }

    @Test
    void supprimerLeveUneExceptionSiLIdentifiantEstInconnu() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(FormationIntrouvableException.class, () -> service.supprimer(99));

        verify(repository, never()).delete(any(Formation.class));
    }

    @Test
    void supprimerRefuseSiLaFormationADesInscriptions() {
        Formation formation = new Formation();
        ReflectionTestUtils.setField(formation, "id", 5);
        when(repository.findById(5)).thenReturn(Optional.of(formation));
        when(repository.compterToutesInscriptions(5)).thenReturn(3L);

        FormationAvecInscriptionsException ex = assertThrows(FormationAvecInscriptionsException.class,
                () -> service.supprimer(5));

        assertEquals("La formation 5 ne peut pas être supprimée : elle compte 3 inscription(s)", ex.getMessage());
        verify(repository, never()).delete(any(Formation.class));
    }

    // ---------- Étudiants d'une formation ----------

    @Test
    void trouverEtudiantsRenvoieLesEtudiantsInscrits() {
        when(repository.existsById(5)).thenReturn(true);
        when(repository.trouverEtudiantsActifs(5)).thenReturn(List.of(
                unEtudiant(1, "Alaoui", "Rania", "rania@example.ma"),
                unEtudiant(2, "Benali", "Yassine", "yassine@example.ma")));

        List<EtudiantResponse> reponses = service.trouverEtudiants(5);

        assertEquals(2, reponses.size());
        assertEquals("Alaoui", reponses.get(0).nom());
        assertEquals("rania@example.ma", reponses.get(0).email());
        assertEquals("Benali", reponses.get(1).nom());
    }

    @Test
    void trouverEtudiantsRenvoieUneListeVideSiPersonneNEstInscrit() {
        when(repository.existsById(5)).thenReturn(true);
        when(repository.trouverEtudiantsActifs(5)).thenReturn(List.of());

        assertTrue(service.trouverEtudiants(5).isEmpty());
    }

    @Test
    void trouverEtudiantsLeveUneExceptionSiLaFormationEstInconnue() {
        when(repository.existsById(99)).thenReturn(false);

        assertThrows(FormationIntrouvableException.class, () -> service.trouverEtudiants(99));

        verify(repository, never()).trouverEtudiantsActifs(any());
    }

    private Etudiant unEtudiant(int id, String nom, String prenom, String email) {
        Etudiant e = new Etudiant();
        ReflectionTestUtils.setField(e, "id", id);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setEmail(email);
        return e;
    }
}
