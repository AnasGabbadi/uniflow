package com.isga.uniflow.formation.service;


import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.formation.dto.FormationResponse;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.repository.FormationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste FormationService avec un repository simulé (Mockito) : pas de base de données.
 */
@ExtendWith(MockitoExtension.class)
class FormationServiceTest {

    @Mock
    private FormationRepository repository;

    @InjectMocks
    private FormationService service;

    private final FormationRequest request = new FormationRequest(
            "  Java avancé ", "Spring Boot", 40, 20,
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20));

    @Test
    void creerEnregistreLaFormationOuverteAvecLesDonneesFournies() {
        when(repository.existsByIntituleIgnoreCase("Java avancé")).thenReturn(false);
        when(repository.save(any(Formation.class))).thenAnswer(inv -> inv.getArgument(0));

        Formation response = service.creer(request);


        ArgumentCaptor<Formation> captor = ArgumentCaptor.forClass(Formation.class);
        verify(repository).save(captor.capture());
        Formation saved = captor.getValue();

        assertEquals("Java avancé", saved.getIntitule());
        assertEquals("Spring Boot", saved.getDescription());
        assertEquals(40, saved.getDuree());
        assertEquals(20, saved.getCapaciteMax());
        assertEquals(LocalDate.of(2026, 12, 1), saved.getDateDebut());
        assertEquals(LocalDate.of(2026, 12, 20), saved.getDateFin());
        assertEquals("OUVERTE", saved.getStatut());

        // La réponse reflète ce qui a été enregistré
        assertEquals("Java avancé", response.getIntitule());
        assertEquals("OUVERTE", response.getStatut());
    }

    @Test
    void creerRefuseUnIntituleDejaUtilise() {
        when(repository.existsByIntituleIgnoreCase("Java avancé")).thenReturn(true);

        assertThrows(FormationDejaExistanteException.class, () -> service.creer(request));
        verify(repository, never()).save(any());
    }
}