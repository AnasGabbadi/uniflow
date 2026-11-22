package com.isga.uniflow.formation.controller;

import com.isga.uniflow.formation.dto.FormationResponse;
import com.isga.uniflow.formation.exception.FormationAvecInscriptionsException;
import com.isga.uniflow.formation.exception.FormationCapaciteInsuffisanteException;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.exception.FormationExceptionHandler;
import com.isga.uniflow.formation.exception.FormationIntrouvableException;
import com.isga.uniflow.formation.service.FormationService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste FormationController et FormationExceptionHandler avec MockMvc en mode "standalone" :
 * ni contexte Spring complet, ni base de données. Le service est simulé avec Mockito.
 */
@ExtendWith(MockitoExtension.class)
class FormationControllerTest {

    private static final String URL = "/api/formations";

    @Mock
    private FormationService formationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FormationController controller = new FormationController(formationService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new FormationExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // ============================================================
    // POST /api/formations
    // ============================================================

    @Test
    void creer_DoitRetourner201AvecFormationCreee() throws Exception {
        when(formationService.creer(any())).thenReturn(uneReponse(42, "Java Spring Boot", 30, 20));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsValide()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/formations/42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.intitule").value("Java Spring Boot"))
                .andExpect(jsonPath("$.description").value("Formation backend Java"))
                .andExpect(jsonPath("$.duree").value(30))
                .andExpect(jsonPath("$.capaciteMax").value(20))
                .andExpect(jsonPath("$.statut").value("OUVERTE"))
                .andExpect(jsonPath("$.placesRestantes").value(20));

        verify(formationService).creer(any());
    }

    @Test
    void creer_AccepteUnStatutFourniEtLeTransmetAuService() throws Exception {
        when(formationService.creer(any())).thenReturn(uneReponse(1, "Java Spring Boot", 30, 20));
        String corps = corpsValide().replace("\"capaciteMax\": 20,", "\"capaciteMax\": 20, \"statut\": \"FERMEE\",");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());
    }

    @Test
    void creer_DoitRetourner400SiStatutInconnu() throws Exception {
        String corps = corpsValide().replace("\"capaciteMax\": 20,", "\"capaciteMax\": 20, \"statut\": \"TERMINEE\",");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.statut")
                        .value("Le statut doit être OUVERTE, FERMEE ou ARCHIVEE"));

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner400SiIntituleEstVide() throws Exception {
        String corps = corpsValide().replace("\"intitule\": \"Java Spring Boot\"", "\"intitule\": \"\"");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Données invalides"))
                .andExpect(jsonPath("$.erreurs.intitule").value("L'intitulé est obligatoire"));

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner400SiCapaciteEstNegative() throws Exception {
        String corps = corpsValide().replace("\"capaciteMax\": 20", "\"capaciteMax\": -5");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.capaciteMax")
                        .value("La capacité doit être strictement positive"));

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner400SiDatesSontIncoherentes() throws Exception {
        String corps = corpsValide()
                .replace("2026-12-01", "2027-02-01")   // début après la fin
                .replace("2027-01-01", "2026-12-01");  // fin

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Données invalides"))
                .andExpect(jsonPath("$.erreurs").exists());

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner400SiChampsObligatoiresAbsents() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.intitule").exists())
                .andExpect(jsonPath("$.erreurs.duree").exists())
                .andExpect(jsonPath("$.erreurs.capaciteMax").exists())
                .andExpect(jsonPath("$.erreurs.dateDebut").exists())
                .andExpect(jsonPath("$.erreurs.dateFin").exists());

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner400SiJsonEstIllisible() throws Exception {
        String corps = """
                {
                    "intitule": "Java Spring Boot",
                    "dateDebut": "01-12-2026"
                }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Corps de requête illisible : vérifiez le JSON et le format des dates (yyyy-MM-dd)"));

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner409SiFormationExisteDeja() throws Exception {
        when(formationService.creer(any()))
                .thenThrow(new FormationDejaExistanteException("Java Spring Boot"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpsValide()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Une formation avec l'intitulé 'Java Spring Boot' existe déjà"));
    }

    // ============================================================
    // PUT /api/formations/{id}
    // ============================================================

    @Test
    void modifier_DoitRetourner200AvecLaFormationMiseAJour() throws Exception {
        when(formationService.modifier(eq(42), any())).thenReturn(uneReponse(42, "Java Spring Boot", 30, 12));

        mockMvc.perform(put(URL + "/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsValide()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.intitule").value("Java Spring Boot"))
                .andExpect(jsonPath("$.placesRestantes").value(12));

        verify(formationService).modifier(eq(42), any());
    }

    @Test
    void modifier_DoitRetourner404SiIdentifiantInconnu() throws Exception {
        when(formationService.modifier(eq(99), any())).thenThrow(new FormationIntrouvableException(99));

        mockMvc.perform(put(URL + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsValide()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucune formation avec l'identifiant 99"));
    }

    @Test
    void modifier_DoitRetourner400SiDonneesInvalides() throws Exception {
        String corps = corpsValide().replace("\"duree\": 30", "\"duree\": 0");

        mockMvc.perform(put(URL + "/42").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.duree").value("La durée doit être strictement positive"));

        verifyNoInteractions(formationService);
    }

    @Test
    void modifier_DoitRetourner400SiDateDeFinPrecedeLaDateDeDebut() throws Exception {
        String corps = corpsValide()
                .replace("2026-12-01", "2027-02-01")
                .replace("2027-01-01", "2026-12-01");

        mockMvc.perform(put(URL + "/42").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.periodeValide")
                        .value("La date de fin ne peut pas précéder la date de début"));

        verifyNoInteractions(formationService);
    }

    @Test
    void modifier_DoitRetourner400SiChampsObligatoiresAbsents() throws Exception {
        mockMvc.perform(put(URL + "/42").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.intitule").exists())
                .andExpect(jsonPath("$.erreurs.capaciteMax").exists());

        verifyNoInteractions(formationService);
    }

    @Test
    void modifier_DoitRetourner400SiIdentifiantNEstPasUnNombre() throws Exception {
        mockMvc.perform(put(URL + "/abc").contentType(MediaType.APPLICATION_JSON).content(corpsValide()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Identifiant invalide : 'abc'"));

        verifyNoInteractions(formationService);
    }

    @Test
    void modifier_DoitRetourner409SiIntituleDejaUtilise() throws Exception {
        when(formationService.modifier(eq(42), any()))
                .thenThrow(new FormationDejaExistanteException("Java Spring Boot"));

        mockMvc.perform(put(URL + "/42").contentType(MediaType.APPLICATION_JSON).content(corpsValide()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Une formation avec l'intitulé 'Java Spring Boot' existe déjà"));
    }

    @Test
    void modifier_DoitRetourner409SiCapaciteInferieureAuxInscriptionsActives() throws Exception {
        when(formationService.modifier(eq(42), any()))
                .thenThrow(new FormationCapaciteInsuffisanteException(10, 15));

        mockMvc.perform(put(URL + "/42").contentType(MediaType.APPLICATION_JSON).content(corpsValide()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "La capacité (10) ne peut pas être inférieure au nombre d'inscriptions actives (15)"));
    }

    // ============================================================
    // GET /api/formations/{id}
    // ============================================================

    @Test
    void trouverParId_DoitRetourner200AvecLesDetailsEtLesPlacesRestantes() throws Exception {
        when(formationService.trouverParId(42)).thenReturn(uneReponse(42, "Java Spring Boot", 30, 7));

        mockMvc.perform(get(URL + "/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.intitule").value("Java Spring Boot"))
                .andExpect(jsonPath("$.description").value("Formation backend Java"))
                .andExpect(jsonPath("$.duree").value(30))
                .andExpect(jsonPath("$.capaciteMax").value(20))
                .andExpect(jsonPath("$.dateDebut").value("2026-12-01"))
                .andExpect(jsonPath("$.dateFin").value("2027-01-01"))
                .andExpect(jsonPath("$.statut").value("OUVERTE"))
                .andExpect(jsonPath("$.placesRestantes").value(7));
    }

    @Test
    void trouverParId_DoitRetourner404SiIdentifiantInconnu() throws Exception {
        when(formationService.trouverParId(99)).thenThrow(new FormationIntrouvableException(99));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Aucune formation avec l'identifiant 99"));
    }

    @Test
    void trouverParId_DoitRetourner400SiIdentifiantNEstPasUnNombre() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Identifiant invalide : 'abc'"));

        verifyNoInteractions(formationService);
    }

    // ============================================================
    // GET /api/formations
    // ============================================================

    @Test
    void trouverToutes_DoitRetourner200AvecToutesLesFormations() throws Exception {
        when(formationService.trouverToutes()).thenReturn(List.of(
                uneReponse(1, "Java Spring Boot", 30, 16),
                uneReponse(2, "Docker et Kubernetes", 40, 25)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].intitule").value("Java Spring Boot"))
                .andExpect(jsonPath("$[0].placesRestantes").value(16))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].intitule").value("Docker et Kubernetes"))
                .andExpect(jsonPath("$[1].placesRestantes").value(25));
    }

    @Test
    void trouverToutes_DoitRetourner200AvecUneListeVideSiAucuneFormation() throws Exception {
        when(formationService.trouverToutes()).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ============================================================
    // Utilitaires
    // ============================================================

    /** Corps JSON valide pour la création d'une formation (sans statut : valeur par défaut). */
    private String corpsValide() {
        return """
                {
                    "intitule": "Java Spring Boot",
                    "description": "Formation backend Java",
                    "duree": 30,
                    "capaciteMax": 20,
                    "dateDebut": "2026-12-01",
                    "dateFin": "2027-01-01"
                }
                """;
    }

    /** Réponse de formation toute prête, telle que la renverrait le service. */
    // ============================================================
    // DELETE /api/formations/{id}
    // ============================================================

    @Test
    void supprimer_DoitRetourner204() throws Exception {
        mockMvc.perform(delete(URL + "/5"))
                .andExpect(status().isNoContent());

        verify(formationService).supprimer(5);
    }

    @Test
    void supprimer_DoitRetourner404SiIdentifiantInconnu() throws Exception {
        doThrow(new FormationIntrouvableException(99)).when(formationService).supprimer(99);

        mockMvc.perform(delete(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void supprimer_DoitRetourner409SiLaFormationADesInscriptions() throws Exception {
        doThrow(new FormationAvecInscriptionsException(5, 3L)).when(formationService).supprimer(5);

        mockMvc.perform(delete(URL + "/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "La formation 5 ne peut pas être supprimée : elle compte 3 inscription(s)"));
    }

    @Test
    void supprimer_DoitRetourner400SiLIdentifiantNEstPasUnNombre() throws Exception {
        mockMvc.perform(delete(URL + "/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(formationService);
    }

    private FormationResponse uneReponse(int id, String intitule, int duree, int placesRestantes) {
        return new FormationResponse(id, intitule, "Formation backend Java", duree, 20,
                LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1), "OUVERTE", placesRestantes);
    }
}
