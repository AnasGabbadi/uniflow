package com.isga.uniflow.etudiant.controller;

import com.isga.uniflow.erreur.CodeErreur;
import com.isga.uniflow.erreur.ConflitMetierException;
import com.isga.uniflow.erreur.GestionnaireErreurs;
import com.isga.uniflow.etudiant.dto.EtudiantResponse;
import com.isga.uniflow.etudiant.service.EtudiantService;
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

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste EtudiantController (POST, IP-23) et GestionnaireErreurs avec MockMvc en mode "standalone" :
 * ni contexte Spring complet, ni base de données. Le service est simulé avec Mockito.
 */
@ExtendWith(MockitoExtension.class)
class EtudiantControllerTest {

    private static final String URL = "/api/etudiants";

    @Mock
    private EtudiantService etudiantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EtudiantController controller = new EtudiantController(etudiantService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GestionnaireErreurs())
                .setValidator(validator)
                .build();
    }

    // ============================================================
    // POST /api/etudiants
    // ============================================================

    @Test
    void creer_DoitRetourner201AvecLEtudiantCree() throws Exception {
        when(etudiantService.creer(any())).thenReturn(uneReponse(42));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsValide()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/etudiants/42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.nom").value("El Amrani"))
                .andExpect(jsonPath("$.prenom").value("Sara"))
                .andExpect(jsonPath("$.email").value("sara.elamrani@example.ma"))
                .andExpect(jsonPath("$.telephone").value("+212 6 12 34 56 78"))
                .andExpect(jsonPath("$.dateNaissance").value("2003-04-21"));

        verify(etudiantService).creer(any());
    }

    @Test
    void creer_DoitRetourner400SiNomEstVide() throws Exception {
        String corps = corpsValide().replace("\"nom\": \"El Amrani\"", "\"nom\": \"\"");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.erreurs[0].champ").value("nom"));

        verifyNoInteractions(etudiantService);
    }

    @Test
    void creer_DoitRetourner400SiEmailInvalide() throws Exception {
        String corps = corpsValide().replace("sara.elamrani@example.ma", "pas-un-email");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.erreurs[0].champ").value("email"));

        verifyNoInteractions(etudiantService);
    }

    @Test
    void creer_DoitRetourner400SiJsonMalForme() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));

        verifyNoInteractions(etudiantService);
    }

    @Test
    void creer_DoitRetourner409SiEmailDejaUtilise() throws Exception {
        when(etudiantService.creer(any())).thenThrow(
                new ConflitMetierException(CodeErreur.EMAIL_DEJA_UTILISE, "Cet email est déjà utilisé."));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpsValide()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_DEJA_UTILISE"));
    }

    // ---------- Données de test ----------

    private String corpsValide() {
        return """
                {
                  "nom": "El Amrani",
                  "prenom": "Sara",
                  "email": "sara.elamrani@example.ma",
                  "telephone": "+212 6 12 34 56 78",
                  "adresse": "12 rue des Écoles, Casablanca",
                  "dateNaissance": "2003-04-21"
                }
                """;
    }

    private EtudiantResponse uneReponse(int id) {
        return new EtudiantResponse(id, "El Amrani", "Sara", "sara.elamrani@example.ma",
                "+212 6 12 34 56 78", "12 rue des Écoles, Casablanca", LocalDate.of(2003, 4, 21));
    }
}
