package com.isga.uniflow.formation.controller;

import com.isga.uniflow.formation.entity.Formation;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        Formation formation = mock(Formation.class);

        when(formation.getId()).thenReturn(42);
        when(formation.getIntitule()).thenReturn("Java Spring Boot");
        when(formation.getDescription()).thenReturn("Formation backend Java");
        when(formation.getDuree()).thenReturn(30);
        when(formation.getCapaciteMax()).thenReturn(20);
        when(formation.getDateDebut()).thenReturn(LocalDate.of(2026, 12, 1));
        when(formation.getDateFin()).thenReturn(LocalDate.of(2027, 1, 1));
        when(formation.getStatut()).thenReturn(Formation.STATUT_OUVERTE);

        when(formationService.creer(any())).thenReturn(formation);

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
                .andExpect(jsonPath("$.statut").value("OUVERTE"));

        verify(formationService).creer(any());
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
    // GET /api/formations/{id}
    // ============================================================

    @Test
    void trouverParId_DoitRetourner200AvecLesDetailsDeLaFormation() throws Exception {
        when(formationService.trouverParId(42))
                .thenReturn(uneFormation(42, "Java Spring Boot"));

        mockMvc.perform(get(URL + "/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.intitule").value("Java Spring Boot"))
                .andExpect(jsonPath("$.description").value("Formation backend Java"))
                .andExpect(jsonPath("$.duree").value(30))
                .andExpect(jsonPath("$.capaciteMax").value(20))
                .andExpect(jsonPath("$.dateDebut").value("2026-12-01"))
                .andExpect(jsonPath("$.dateFin").value("2027-01-01"))
                .andExpect(jsonPath("$.statut").value("OUVERTE"));
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
                uneFormation(1, "Java Spring Boot"),
                uneFormation(2, "Docker et Kubernetes")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].intitule").value("Java Spring Boot"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].intitule").value("Docker et Kubernetes"));
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

    /** Corps JSON valide pour la création d'une formation. */
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

    /** Formation réelle avec un identifiant fixé (l'entité n'a pas de setId : on passe par réflexion). */
    private Formation uneFormation(int id, String intitule) {
        Formation f = new Formation();
        ReflectionTestUtils.setField(f, "id", id);
        f.setIntitule(intitule);
        f.setDescription("Formation backend Java");
        f.setDuree(30);
        f.setCapaciteMax(20);
        f.setDateDebut(LocalDate.of(2026, 12, 1));
        f.setDateFin(LocalDate.of(2027, 1, 1));
        f.setStatut(Formation.STATUT_OUVERTE);
        return f;
    }
}