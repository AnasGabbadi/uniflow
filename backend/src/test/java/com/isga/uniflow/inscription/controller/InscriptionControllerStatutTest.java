package com.isga.uniflow.inscription.controller;

import com.isga.uniflow.inscription.dto.InscriptionResponse;
import com.isga.uniflow.inscription.dto.InscriptionResponse.EtudiantResume;
import com.isga.uniflow.inscription.dto.InscriptionResponse.FormationResume;
import com.isga.uniflow.inscription.entity.StatutInscription;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste PATCH /api/inscriptions/{id}/statut (changement de statut, dont l'annulation) avec MockMvc
 * en mode "standalone" : ni contexte Spring complet, ni base de données.
 *
 * Les statuts 404 (inscription inconnue) et 409 (transition interdite) dépendent du gestionnaire
 * d'erreurs global de l'équipe : ils sont vérifiés au niveau du service (InscriptionServiceStatutTest).
 */
@ExtendWith(MockitoExtension.class)
class InscriptionControllerStatutTest {

    private static final String URL = "/api/inscriptions/7/statut";

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
    void annuler_DoitRetourner200AvecLeStatutAnnulee() throws Exception {
        when(inscriptionService.changerStatut(7, StatutInscription.ANNULEE))
                .thenReturn(uneReponse("ANNULEE"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\": \"ANNULEE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.statut").value("ANNULEE"))
                .andExpect(jsonPath("$.etudiant.nom").value("Benali"))
                .andExpect(jsonPath("$.formation.intitule").value("Java avancé"));

        verify(inscriptionService).changerStatut(7, StatutInscription.ANNULEE);
    }

    @Test
    void valider_DoitRetourner200AvecLeStatutValidee() throws Exception {
        when(inscriptionService.changerStatut(7, StatutInscription.VALIDEE))
                .thenReturn(uneReponse("VALIDEE"));

        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\": \"VALIDEE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDEE"));
    }

    @Test
    void changerStatut_DoitRetourner400SiLeStatutEstAbsent() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inscriptionService);
    }

    @Test
    void changerStatut_DoitRetourner400SiLeStatutEstInconnu() throws Exception {
        mockMvc.perform(patch(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\": \"TERMINEE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inscriptionService);
    }

    @Test
    void changerStatut_DoitRetourner400SiLIdentifiantNEstPasUnNombre() throws Exception {
        mockMvc.perform(patch("/api/inscriptions/abc/statut")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\": \"ANNULEE\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(inscriptionService);
    }

    private InscriptionResponse uneReponse(String statut) {
        return new InscriptionResponse(
                7,
                new EtudiantResume(1, "Benali", "Yassine", "yassine.benali@mail.com"),
                new FormationResume(3, "Java avancé"),
                LocalDate.of(2026, 11, 19),
                statut);
    }
}
