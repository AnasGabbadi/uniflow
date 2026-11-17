package com.isga.uniflow.formation.service;

import com.isga.uniflow.formation.dto.FormationRequest;
import com.isga.uniflow.formation.entity.Formation;
import com.isga.uniflow.formation.exception.FormationDejaExistanteException;
import com.isga.uniflow.formation.exception.FormationIntrouvableException;
import com.isga.uniflow.formation.repository.FormationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    // ---------- Création ----------

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

    // ---------- Consultation par identifiant ----------

    @Test
    void trouverParIdRenvoieLaFormationExistante() {
        Formation formation = uneFormation("Java avancé");
        when(repository.findById(1)).thenReturn(Optional.of(formation));

        assertSame(formation, service.trouverParId(1));
    }

    @Test
    void trouverParIdLeveUneExceptionSiLIdentifiantEstInconnu() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        FormationIntrouvableException ex =
                assertThrows(FormationIntrouvableException.class, () -> service.trouverParId(99));

        assertEquals("Aucune formation avec l'identifiant 99", ex.getMessage());
    }

    // ---------- Consultation de la liste ----------

    @Test
    void trouverToutesRenvoieLesFormationsTrieesParIdentifiant() {
        Formation f1 = uneFormation("Java avancé");
        Formation f2 = uneFormation("Docker et Kubernetes");
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(f1, f2));

        List<Formation> resultat = service.trouverToutes();

        assertEquals(2, resultat.size());
        assertSame(f1, resultat.get(0));
        assertSame(f2, resultat.get(1));
    }

    @Test
    void trouverToutesRenvoieUneListeVideQuandIlNYADeFormation() {
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of());

        assertTrue(service.trouverToutes().isEmpty());
    }

    // ---------- Utilitaire ----------

    private Formation uneFormation(String intitule) {
        Formation f = new Formation();
        f.setIntitule(intitule);
        f.setDuree(40);
        f.setCapaciteMax(20);
        f.setDateDebut(LocalDate.of(2026, 12, 1));
        f.setDateFin(LocalDate.of(2026, 12, 20));
        return f;
    }
}