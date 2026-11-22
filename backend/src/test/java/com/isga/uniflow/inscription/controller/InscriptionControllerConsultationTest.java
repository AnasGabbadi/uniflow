package com.isga.uniflow.inscription.controller;

import com.isga.uniflow.inscription.dto.InscriptionResponse;
import com.isga.uniflow.inscription.dto.InscriptionResponse.EtudiantResume;
import com.isga.uniflow.inscription.dto.InscriptionResponse.FormationResume;
import com.isga.uniflow.inscription.service.InscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste les endpoints de consultation (GET) avec MockMvc en mode "standalone" : ni contexte Spring
 * complet, ni base de données. Le service est simulé avec Mockito.
 *
 * Le statut 404 pour un identifiant inconnu dépend du gestionnaire d'erreurs global de l'équipe :
 * il est vérifié au niveau du service (InscriptionServiceConsultationTest), qui lève l'exception.
 */
@ExtendWith(MockitoExtension.class)
class InscriptionControllerConsultationTest {

    private static final String URL = "/api/inscriptions";

    @Mock
    private InscriptionService inscriptionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InscriptionController(inscriptionService))
                .build();
    }

    @Test
    void lister_DoitRetourner200AvecToutesLesInscriptions() throws Exception {
        when(inscriptionService.lister()).thenReturn(List.of(
                uneReponse(1, 1, "Benali", "Yassine", 3, "Java avancé", "VALIDEE"),
                uneReponse(2, 2, "Haddad", "Nour", 4, "Data Science", "EN_ATTENTE")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].etudiant.id").value(1))
                .andExpect(jsonPath("$[0].etudiant.nom").value("Benali"))
                .andExpect(jsonPath("$[0].formation.id").value(3))
                .andExpect(jsonPath("$[0].formation.intitule").value("Java avancé"))
                .andExpect(jsonPath("$[0].statut").value("VALIDEE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].etudiant.nom").value("Haddad"))
                .andExpect(jsonPath("$[1].formation.intitule").value("Data Science"))
                .andExpect(jsonPath("$[1].statut").value("EN_ATTENTE"));
    }

    @Test
    void lister_DoitRetourner200AvecUneListeVideSiAucuneInscription() throws Exception {
        when(inscriptionService.lister()).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void trouverParId_DoitRetourner200AvecLesDetailsSansRecursion() throws Exception {
        when(inscriptionService.trouverParId(7))
                .thenReturn(uneReponse(7, 1, "Benali", "Yassine", 3, "Java avancé", "VALIDEE"));

        mockMvc.perform(get(URL + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.etudiant.id").value(1))
                .andExpect(jsonPath("$.etudiant.nom").value("Benali"))
                .andExpect(jsonPath("$.etudiant.prenom").value("Yassine"))
                .andExpect(jsonPath("$.etudiant.email").value("yassine.benali@mail.com"))
                .andExpect(jsonPath("$.formation.id").value(3))
                .andExpect(jsonPath("$.formation.intitule").value("Java avancé"))
                .andExpect(jsonPath("$.dateInscription").value("2026-11-19"))
                .andExpect(jsonPath("$.statut").value("VALIDEE"))
                // Pas de sérialisation récursive : ni les inscriptions de l'étudiant, ni celles de la formation
                .andExpect(jsonPath("$.etudiant.inscriptions").doesNotExist())
                .andExpect(jsonPath("$.formation.inscriptions").doesNotExist());
    }

    @Test
    void trouverParId_DoitRetourner400SiLIdentifiantNEstPasUnNombre() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inscriptionService);
    }

    private InscriptionResponse uneReponse(int id, int idEtudiant, String nom, String prenom,
                                           int idFormation, String intitule, String statut) {
        return new InscriptionResponse(
                id,
                new EtudiantResume(idEtudiant, nom, prenom,
                        prenom.toLowerCase() + "." + nom.toLowerCase() + "@mail.com"),
                new FormationResume(idFormation, intitule),
                LocalDate.of(2026, 11, 19),
                statut);
    }
}
