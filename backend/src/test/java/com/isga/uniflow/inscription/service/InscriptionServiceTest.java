package com.isga.uniflow.inscription.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste la création d'une inscription avec des repositories simulés (Mockito) : pas de base de données.
 */
@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {

    /** Statuts qui occupent une place. */
    private static final List<String> STATUTS_ACTIFS = List.of("EN_ATTENTE", "VALIDEE");

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private FormationRepository formationRepository;

    @InjectMocks
    private InscriptionService service;

    private final InscriptionRequest request = new InscriptionRequest(1, 3);

    // ---------- Succès ----------

    @Test
    void creerEnregistreUneInscriptionEnAttenteAvecLEtudiantEtLaFormation() {
        Etudiant etudiant = unEtudiant(1, "Benali", "Yassine");
        Formation formation = uneFormation(3, "Java avancé", 20);
        when(etudiantRepository.findById(1)).thenReturn(Optional.of(etudiant));
        when(formationRepository.findById(3)).thenReturn(Optional.of(formation));
        when(inscriptionRepository.existsByEtudiant_IdAndFormation_Id(1, 3)).thenReturn(false);
        when(inscriptionRepository.countByFormation_IdAndStatutIn(3, STATUTS_ACTIFS)).thenReturn(5L);
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        InscriptionResponse response = service.creer(request);

        ArgumentCaptor<Inscription> captor = ArgumentCaptor.forClass(Inscription.class);
        verify(inscriptionRepository).save(captor.capture());
        Inscription saved = captor.getValue();

        assertSame(etudiant, saved.getEtudiant());
        assertSame(formation, saved.getFormation());
        assertEquals("EN_ATTENTE", saved.getStatut());

        // La réponse expose des résumés (pas d'entités) de l'étudiant et de la formation
        assertEquals(1, response.etudiant().id());
        assertEquals("Benali", response.etudiant().nom());
        assertEquals("Yassine", response.etudiant().prenom());
        assertEquals(3, response.formation().id());
        assertEquals("Java avancé", response.formation().intitule());
        assertEquals("EN_ATTENTE", response.statut());
    }

    @Test
    void creerAccepteLaDerniereDisponiblePlace() {
        // Capacité 20, 19 inscriptions actives : il reste exactement une place
        when(etudiantRepository.findById(1)).thenReturn(Optional.of(unEtudiant(1, "Benali", "Yassine")));
        when(formationRepository.findById(3)).thenReturn(Optional.of(uneFormation(3, "Java avancé", 20)));
        when(inscriptionRepository.existsByEtudiant_IdAndFormation_Id(1, 3)).thenReturn(false);
        when(inscriptionRepository.countByFormation_IdAndStatutIn(3, STATUTS_ACTIFS)).thenReturn(19L);
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        InscriptionResponse response = service.creer(request);

        assertEquals("EN_ATTENTE", response.statut());
    }

    // ---------- Références inexistantes : 404 ----------

    @Test
    void creerLeveUneExceptionSiLEtudiantEstInconnu() {
        when(etudiantRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> service.creer(request));
        verify(inscriptionRepository, never()).save(any());
    }

    @Test
    void creerLeveUneExceptionSiLaFormationEstInconnue() {
        when(etudiantRepository.findById(1)).thenReturn(Optional.of(unEtudiant(1, "Benali", "Yassine")));
        when(formationRepository.findById(3)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> service.creer(request));
        verify(inscriptionRepository, never()).save(any());
    }

    // ---------- Doublon : 409 ----------

    @Test
    void creerRefuseUneInscriptionEnDoublon() {
        when(etudiantRepository.findById(1)).thenReturn(Optional.of(unEtudiant(1, "Benali", "Yassine")));
        when(formationRepository.findById(3)).thenReturn(Optional.of(uneFormation(3, "Java avancé", 20)));
        when(inscriptionRepository.existsByEtudiant_IdAndFormation_Id(1, 3)).thenReturn(true);

        ConflitMetierException ex = assertThrows(ConflitMetierException.class, () -> service.creer(request));

        assertEquals("L'étudiant Yassine Benali est déjà inscrit à la formation 'Java avancé'.",
                ex.getMessage());
        verify(inscriptionRepository, never()).save(any());
    }

    // ---------- Capacité : 409 ----------

    @Test
    void creerRefuseSiLaFormationEstComplete() {
        when(etudiantRepository.findById(1)).thenReturn(Optional.of(unEtudiant(1, "Benali", "Yassine")));
        when(formationRepository.findById(3)).thenReturn(Optional.of(uneFormation(3, "Java avancé", 20)));
        when(inscriptionRepository.existsByEtudiant_IdAndFormation_Id(1, 3)).thenReturn(false);
        when(inscriptionRepository.countByFormation_IdAndStatutIn(3, STATUTS_ACTIFS)).thenReturn(20L);

        ConflitMetierException ex = assertThrows(ConflitMetierException.class, () -> service.creer(request));

        assertEquals("La formation 'Java avancé' est complète (capacité : 20).", ex.getMessage());
        verify(inscriptionRepository, never()).save(any());
    }

    // ---------- Utilitaires ----------

    /** L'entité n'a pas de setId : on fixe l'identifiant par réflexion. */
    private Etudiant unEtudiant(int id, String nom, String prenom) {
        Etudiant e = new Etudiant();
        ReflectionTestUtils.setField(e, "id", id);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setEmail(prenom.toLowerCase() + "." + nom.toLowerCase() + "@mail.com");
        return e;
    }

    private Formation uneFormation(int id, String intitule, int capaciteMax) {
        Formation f = new Formation();
        ReflectionTestUtils.setField(f, "id", id);
        f.setIntitule(intitule);
        f.setCapaciteMax(capaciteMax);
        return f;
    }
}
