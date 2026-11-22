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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste InscriptionController avec MockMvc en mode "standalone" : ni contexte Spring complet,
 * ni base de données. Le service est simulé avec Mockito.
 *
 * Le contrôleur est testé SANS le gestionnaire d'erreurs global de l'équipe : les statuts 404 et 409
 * (qui dépendent de ce gestionnaire) sont vérifiés au niveau du service, qui lève les exceptions.
 */
@ExtendWith(MockitoExtension.class)
class InscriptionControllerTest {

    private static final String URL = "/api/inscriptions";

    @Mock
    private InscriptionService inscriptionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new InscriptionController(inscriptionService))
                .setValidator(validator)
                .build();
    }

    @Test
    void inscrire_DoitRetourner201AvecLocationEtLesResumesSansRecursion() throws Exception {
        when(inscriptionService.creer(any())).thenReturn(uneReponse());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEtudiant\": 1, \"idFormation\": 3}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/inscriptions/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.etudiant.id").value(1))
                .andExpect(jsonPath("$.etudiant.nom").value("Benali"))
                .andExpect(jsonPath("$.etudiant.prenom").value("Yassine"))
                .andExpect(jsonPath("$.formation.id").value(3))
                .andExpect(jsonPath("$.formation.intitule").value("Java avancé"))
                .andExpect(jsonPath("$.dateInscription").value("2026-11-19"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                // Pas de sérialisation récursive : l'étudiant n'expose pas ses inscriptions
                .andExpect(jsonPath("$.etudiant.inscriptions").doesNotExist());

        verify(inscriptionService).creer(any());
    }

    @Test
    void inscrire_DoitRetourner400SiLIdentifiantDeLEtudiantEstAbsent() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idFormation\": 3}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inscriptionService);
    }

    @Test
    void inscrire_DoitRetourner400SiLIdentifiantDeLaFormationEstAbsent() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idEtudiant\": 1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inscriptionService);
    }

    @Test
    void inscrire_DoitRetourner400SiLeCorpsEstVide() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inscriptionService);
    }

    private InscriptionResponse uneReponse() {
        return new InscriptionResponse(
                7,
                new EtudiantResume(1, "Benali", "Yassine", "yassine.benali@mail.com"),
                new FormationResume(3, "Java avancé"),
                LocalDate.of(2026, 11, 19),
                "EN_ATTENTE");
    }
}
