package com.isga.uniflow.etudiant.controller;

import com.isga.uniflow.erreur.CodeErreur;
import com.isga.uniflow.erreur.ConflitMetierException;
import com.isga.uniflow.erreur.GestionnaireErreurs;
import com.isga.uniflow.erreur.RessourceIntrouvableException;
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
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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

    // ============================================================
    // GET /api/etudiants
    // ============================================================

    @Test
    void lister_DoitRetourner200AvecLaListeDesEtudiants() throws Exception {
        when(etudiantService.lister(null)).thenReturn(List.of(uneReponse(1), uneReponse(2)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("El Amrani"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void lister_DoitTransmettreLeParametreDeRechercheAuService() throws Exception {
        when(etudiantService.lister("ali")).thenReturn(List.of(uneReponse(3)));

        mockMvc.perform(get(URL).param("recherche", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3));

        verify(etudiantService).lister("ali");
    }

    @Test
    void lister_DoitRetourner200EtUneListeVideSiAucunEtudiant() throws Exception {
        when(etudiantService.lister(null)).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void lister_DoitTransmettreUneRechercheParNomComplet() throws Exception {
        when(etudiantService.lister("sara el amrani")).thenReturn(List.of(uneReponse(1)));

        mockMvc.perform(get(URL).param("recherche", "sara el amrani"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nom").value("El Amrani"));

        verify(etudiantService).lister("sara el amrani");
    }

    // ============================================================
    // GET /api/etudiants/{id}
    // ============================================================

    @Test
    void trouverParId_DoitRetourner200AvecLeDetailDeLEtudiant() throws Exception {
        when(etudiantService.trouverParId(42)).thenReturn(uneReponse(42));

        mockMvc.perform(get(URL + "/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.nom").value("El Amrani"))
                .andExpect(jsonPath("$.email").value("sara.elamrani@example.ma"))
                .andExpect(jsonPath("$.dateNaissance").value("2003-04-21"));
    }

    @Test
    void trouverParId_DoitRetourner404SiIdentifiantInconnu() throws Exception {
        when(etudiantService.trouverParId(99)).thenThrow(new RessourceIntrouvableException("étudiant", 99));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"))
                .andExpect(jsonPath("$.detail").value("Aucun étudiant avec l'identifiant 99."));
    }

    @Test
    void trouverParId_DoitRetourner400SiIdentifiantNonNumerique() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));

        verifyNoInteractions(etudiantService);
    }

    // ============================================================
    // PUT /api/etudiants/{id}
    // ============================================================

    @Test
    void modifier_DoitRetourner200AvecLEtudiantModifie() throws Exception {
        when(etudiantService.modifier(eq(42), any())).thenReturn(uneReponse(42));

        mockMvc.perform(put(URL + "/42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpsValide()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.nom").value("El Amrani"))
                .andExpect(jsonPath("$.email").value("sara.elamrani@example.ma"));

        verify(etudiantService).modifier(eq(42), any());
    }

    @Test
    void modifier_DoitRetourner400SiDonneesInvalides() throws Exception {
        String corps = corpsValide().replace("sara.elamrani@example.ma", "pas-un-email");

        mockMvc.perform(put(URL + "/42").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.erreurs[0].champ").value("email"));

        verifyNoInteractions(etudiantService);
    }

    @Test
    void modifier_DoitRetourner404SiIdentifiantInconnu() throws Exception {
        when(etudiantService.modifier(eq(99), any()))
                .thenThrow(new RessourceIntrouvableException("étudiant", 99));

        mockMvc.perform(put(URL + "/99").contentType(MediaType.APPLICATION_JSON).content(corpsValide()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"));
    }

    @Test
    void modifier_DoitRetourner409SiEmailUtiliseParUnAutreEtudiant() throws Exception {
        when(etudiantService.modifier(eq(42), any())).thenThrow(
                new ConflitMetierException(CodeErreur.EMAIL_DEJA_UTILISE, "Cet email est déjà utilisé."));

        mockMvc.perform(put(URL + "/42").contentType(MediaType.APPLICATION_JSON).content(corpsValide()))
                .andExpect(status().isConflict())
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
