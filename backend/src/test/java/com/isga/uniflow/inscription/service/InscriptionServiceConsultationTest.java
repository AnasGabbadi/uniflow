package com.isga.uniflow.inscription.service;

import com.isga.uniflow.erreur.RessourceIntrouvableException;
import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.etudiant.repository.EtudiantRepository;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.repository.FormationRepository;
import com.isga.uniflow.inscription.dto.InscriptionResponse;
import com.isga.uniflow.inscription.entity.Inscription;
import com.isga.uniflow.inscription.repository.InscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Teste la consultation des inscriptions (liste et par identifiant), avec des repositories simulés.
 */
@ExtendWith(MockitoExtension.class)
class InscriptionServiceConsultationTest {

    @Mock
    private InscriptionRepository inscriptionRepository;

    // Non utilisés ici, mais requis pour construire InscriptionService
    @Mock
    private EtudiantRepository etudiantRepository;

    @Mock
    private FormationRepository formationRepository;

    @InjectMocks
    private InscriptionService service;

    // ---------- Liste ----------

    @Test
    void listerRenvoieToutesLesInscriptionsAvecLesResumesEtudiantEtFormation() {
        when(inscriptionRepository.findAllAvecDetails()).thenReturn(List.of(
                uneInscription(1, 1, "Benali", "Yassine", 3, "Java avancé", "VALIDEE"),
                uneInscription(2, 2, "Haddad", "Nour", 3, "Java avancé", "EN_ATTENTE")));

        List<InscriptionResponse> resultat = service.lister();

        assertEquals(2, resultat.size());

        assertEquals(1, resultat.get(0).id());
        assertEquals(1, resultat.get(0).etudiant().id());
        assertEquals("Benali", resultat.get(0).etudiant().nom());
        assertEquals(3, resultat.get(0).formation().id());
        assertEquals("Java avancé", resultat.get(0).formation().intitule());
        assertEquals("VALIDEE", resultat.get(0).statut());

        assertEquals(2, resultat.get(1).id());
        assertEquals("Haddad", resultat.get(1).etudiant().nom());
        assertEquals("EN_ATTENTE", resultat.get(1).statut());
    }

    @Test
    void listerRenvoieUneListeVideQuandIlNYADInscription() {
        when(inscriptionRepository.findAllAvecDetails()).thenReturn(List.of());

        assertTrue(service.lister().isEmpty());
    }

    // ---------- Par identifiant ----------

    @Test
    void trouverParIdRenvoieLInscriptionAvecSesDetails() {
        when(inscriptionRepository.findByIdAvecDetails(7)).thenReturn(
                Optional.of(uneInscription(7, 1, "Benali", "Yassine", 3, "Java avancé", "VALIDEE")));

        InscriptionResponse response = service.trouverParId(7);

        assertEquals(7, response.id());
        assertEquals("Yassine", response.etudiant().prenom());
        assertEquals("yassine.benali@mail.com", response.etudiant().email());
        assertEquals("Java avancé", response.formation().intitule());
        assertEquals(LocalDate.of(2026, 11, 19), response.dateInscription());
        assertEquals("VALIDEE", response.statut());
    }

    @Test
    void trouverParIdLeveUneExceptionSiLIdentifiantEstInconnu() {
        when(inscriptionRepository.findByIdAvecDetails(99)).thenReturn(Optional.empty());

        assertThrows(RessourceIntrouvableException.class, () -> service.trouverParId(99));
    }

    // ---------- Utilitaire ----------

    private Inscription uneInscription(int id, int idEtudiant, String nom, String prenom,
                                       int idFormation, String intitule, String statut) {
        Etudiant etudiant = new Etudiant();
        ReflectionTestUtils.setField(etudiant, "id", idEtudiant);
        etudiant.setNom(nom);
        etudiant.setPrenom(prenom);
        etudiant.setEmail(prenom.toLowerCase() + "." + nom.toLowerCase() + "@mail.com");

        Formation formation = new Formation();
        ReflectionTestUtils.setField(formation, "id", idFormation);
        formation.setIntitule(intitule);
        formation.setCapaciteMax(20);

        Inscription inscription = new Inscription();
        ReflectionTestUtils.setField(inscription, "id", id);
        inscription.setEtudiant(etudiant);
        inscription.setFormation(formation);
        inscription.setStatut(statut);
        inscription.setDateInscription(LocalDate.of(2026, 11, 19));
        return inscription;
    }
}
