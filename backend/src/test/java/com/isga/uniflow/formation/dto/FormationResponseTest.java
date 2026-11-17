
package com.isga.uniflow.formation.dto;

import com.isga.uniflow.formation.entity.Formation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormationResponseTest {

    @Test
    void from_ShouldMapAllFormationFieldsToResponse() {

        // Arrange
        Formation formation = mock(Formation.class);

        LocalDate dateDebut = LocalDate.of(2026, 12, 1);
        LocalDate dateFin = LocalDate.of(2027, 3, 1);

        when(formation.getId()).thenReturn(1);
        when(formation.getIntitule()).thenReturn("Java Spring Boot");
        when(formation.getDescription()).thenReturn("Formation backend Java");
        when(formation.getDuree()).thenReturn(90);
        when(formation.getCapaciteMax()).thenReturn(25);
        when(formation.getDateDebut()).thenReturn(dateDebut);
        when(formation.getDateFin()).thenReturn(dateFin);
        when(formation.getStatut()).thenReturn("OUVERTE");

        FormationResponse expected = new FormationResponse(
                1,
                "Java Spring Boot",
                "Formation backend Java",
                90,
                25,
                dateDebut,
                dateFin,
                "OUVERTE"
        );

        // Act
        FormationResponse actual = FormationResponse.from(formation);

        // Assert
        assertEquals(expected, actual);
    }
}
