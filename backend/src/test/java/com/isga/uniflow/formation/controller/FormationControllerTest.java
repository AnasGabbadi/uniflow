package com.isga.uniflow.formation.controller;

import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.exception.FormationExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FormationControllerTest {

    @Mock
    private FormationService formationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        FormationController controller =
                new FormationController(formationService);

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new FormationExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void creer_DoitRetourner201AvecFormationCreee() throws Exception {

        // Arrange
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

        String requestBody = """
                {
                    "intitule": "Java Spring Boot",
                    "description": "Formation backend Java",
                    "duree": 30,
                    "capaciteMax": 20,
                    "dateDebut": "2026-12-01",
                    "dateFin": "2027-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/formations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.endsWith("/api/formations/42")
                ))
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

        // Arrange
        String requestBody = """
                {
                    "intitule": "",
                    "description": "Formation backend Java",
                    "duree": 30,
                    "capaciteMax": 20,
                    "dateDebut": "2026-12-01",
                    "dateFin": "2027-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/formations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Données invalides"))
                .andExpect(jsonPath("$.erreurs.intitule")
                        .value("L'intitulé est obligatoire"));

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner400SiDatesSontIncoherentes() throws Exception {

        // Arrange
        String requestBody = """
                {
                    "intitule": "Java Spring Boot",
                    "description": "Formation backend Java",
                    "duree": 30,
                    "capaciteMax": 20,
                    "dateDebut": "2027-01-01",
                    "dateFin": "2026-12-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/formations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Données invalides"))
                .andExpect(jsonPath("$.erreurs").exists());

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner400SiJsonEstIllisible() throws Exception {

        // Arrange
        String requestBody = """
                {
                    "intitule": "Java Spring Boot",
                    "dateDebut": "01-12-2026"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/formations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Corps de requête illisible : vérifiez le JSON et le format des dates (yyyy-MM-dd)"
                ));

        verifyNoInteractions(formationService);
    }

    @Test
    void creer_DoitRetourner409SiFormationExisteDeja() throws Exception {

        // Arrange
        when(formationService.creer(any()))
                .thenThrow(new FormationDejaExistanteException("Java Spring Boot"));

        String requestBody = """
                {
                    "intitule": "Java Spring Boot",
                    "description": "Formation backend Java",
                    "duree": 30,
                    "capaciteMax": 20,
                    "dateDebut": "2026-12-01",
                    "dateFin": "2027-01-01"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/formations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Une formation avec l'intitulé 'Java Spring Boot' existe déjà"
                ));
    }
}
