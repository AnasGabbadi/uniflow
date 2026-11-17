
package com.isga.uniflow.formation.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FormationTest {

    @Test
    void constructor_ShouldInitializeDefaultValues() {
        Formation formation = new Formation();

        assertNull(formation.getId());
        assertEquals(Formation.STATUT_OUVERTE, formation.getStatut());
        assertNotNull(formation.getInscriptions());
        assertTrue(formation.getInscriptions().isEmpty());
    }

    @Test
    void settersAndGetters_ShouldHandleAllFields() {
        Formation formation = new Formation();

        LocalDate debut = LocalDate.of(2026, 12, 1);
        LocalDate fin = LocalDate.of(2027, 3, 1);

        formation.setIntitule("Java Spring Boot");
        formation.setDescription("Backend development");
        formation.setDuree(90);
        formation.setCapaciteMax(25);
        formation.setDateDebut(debut);
        formation.setDateFin(fin);
        formation.setStatut("FERMEE");

        assertEquals("Java Spring Boot", formation.getIntitule());
        assertEquals("Backend development", formation.getDescription());
        assertEquals(90, formation.getDuree());
        assertEquals(25, formation.getCapaciteMax());
        assertEquals(debut, formation.getDateDebut());
        assertEquals(fin, formation.getDateFin());
        assertEquals("FERMEE", formation.getStatut());
    }
}
