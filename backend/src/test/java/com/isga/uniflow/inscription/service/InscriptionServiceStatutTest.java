package com.isga.uniflow.inscription.service;

import com.isga.uniflow.erreur.ConflitMetierException;
import com.isga.uniflow.erreur.RessourceIntrouvableException;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.repository.FormationRepository;
import com.isga.uniflow.inscription.dto.InscriptionResponse;
import com.isga.uniflow.inscription.entity.Inscription;
import com.isga.uniflow.inscription.entity.StatutInscription;
import com.isga.uniflow.inscription.repository.InscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste le changement de statut d'une inscription (cycle de vie), avec des repositories simulés.
 */
@ExtendWith(MockitoExtension.class)
class InscriptionServiceStatutTest {

    @Mock
    private InscriptionRepository inscriptionRepository;

    // Non utilisés ici, mais requis pour construire InscriptionService
    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private FormationRepository formationRepository;

    @InjectMocks
    private InscriptionService service;

    @Test
    void validerUneInscriptionEnAttente() {
        when(inscriptionRepository.findById(7)).thenReturn(Optional.of(uneInscription(7, "EN_ATTENTE")));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        InscriptionResponse response = service.changerStatut(7, StatutInscription.VALIDEE);

        assertEquals("VALIDEE", response.statut());
        assertEquals(7, response.id());
    }

    @Test
    void annulerUneInscriptionEnAttente() {
        when(inscriptionRepository.findById(7)).thenReturn(Optional.of(uneInscription(7, "EN_ATTENTE")));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("ANNULEE", service.changerStatut(7, StatutInscription.ANNULEE).statut());
    }

    @Test
    void annulerUneInscriptionValidee() {
        when(inscriptionRepository.findById(7)).thenReturn(Optional.of(uneInscription(7, "VALIDEE")));
        when(inscriptionRepository.save(any(Inscription.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("ANNULEE", service.changerStatut(7, StatutInscription.ANNULEE).statut());
    }

    @Test
    void refuseDeReactiverUneInscriptionAnnulee() {
        when(inscriptionRepository.findById(7)).thenReturn(Optional.of(uneInscription(7, "ANNULEE")));

        ConflitMetierException ex = assertThrows(ConflitMetierException.class,
                () -> service.changerStatut(7, StatutInscription.VALIDEE));

        assertEquals("Transition de statut interdite : ANNULEE vers VALIDEE.", ex.getMessage());
        verify(inscriptionRepository, never()).save(any());
    }

    @Test
    void refuseDeRevenirDeValideeVersEnAttente() {
        when(inscriptionRepository.findById(7)).thenReturn(Optional.of(uneInscription(7, "VALIDEE")));

        assertThrows(ConflitMetierException.class,
                () -> service.changerStatut(7, StatutInscription.EN_ATTENTE));
        verify(inscriptionRepository, never()).save(any());
    }

    @Test
    void refuseDeRepasserDansLeMemeStatut() {
        when(inscriptionRepository.findById(7)).thenReturn(Optional.of(uneInscription(7, "EN_ATTENTE")));

        assertThrows(ConflitMetierException.class,
                () -> service.changerStatut(7, StatutInscription.EN_ATTENTE));
        verify(inscriptionRepository, never()).save(any());
    }

    @Test
    void leveUneExceptionSiLInscriptionEstInconnue() {
        when(inscriptionRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class,
                () -> service.changerStatut(99, StatutInscription.ANNULEE));
        verify(inscriptionRepository, never()).save(any());
    }

    // ---------- Utilitaire ----------

    private Inscription uneInscription(int id, String statut) {
        Etudiant etudiant = new Etudiant();
        ReflectionTestUtils.setField(etudiant, "id", 1);
        etudiant.setNom("Benali");
        etudiant.setPrenom("Yassine");
        etudiant.setEmail("yassine.benali@mail.com");

        Formation formation = new Formation();
        ReflectionTestUtils.setField(formation, "id", 3);
        formation.setIntitule("Java avancé");
        formation.setCapaciteMax(20);

        Inscription inscription = new Inscription();
        ReflectionTestUtils.setField(inscription, "id", id);
        inscription.setEtudiant(etudiant);
        inscription.setFormation(formation);
        inscription.setStatut(statut);
        return inscription;
    }
}
