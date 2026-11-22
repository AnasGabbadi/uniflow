
package com.isga.uniflow.etudiant.entity;

import com.isga.uniflow.inscription.entity.Inscription;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EtudiantTest {

    @Test
    void constructor_ShouldInitializeEmptyInscriptions() {
        Etudiant etudiant = new Etudiant();

        assertNull(etudiant.getId());
        assertNotNull(etudiant.getInscriptions());
        assertTrue(etudiant.getInscriptions().isEmpty());
    }

    @Test
    void gettersAndSetters_ShouldHandleAllFields() {
        Etudiant etudiant = new Etudiant();

        LocalDate dateNaissance = LocalDate.of(2000, 5, 15);

        etudiant.setNom("Dupont");
        etudiant.setPrenom("Jean");
        etudiant.setEmail("jean.dupont@example.com");
        etudiant.setTelephone("0612345678");
        etudiant.setAdresse("Fes, Morocco");
        etudiant.setDateNaissance(dateNaissance);

        assertEquals("Dupont", etudiant.getNom());
        assertEquals("Jean", etudiant.getPrenom());
        assertEquals("jean.dupont@example.com", etudiant.getEmail());
        assertEquals("0612345678", etudiant.getTelephone());
        assertEquals("Fes, Morocco", etudiant.getAdresse());
        assertEquals(dateNaissance, etudiant.getDateNaissance());
    }

    @Test
    void inscriptions_ShouldReturnSameListInstance() {
        Etudiant etudiant = new Etudiant();

        List<Inscription> inscriptions = etudiant.getInscriptions();

        assertSame(inscriptions, etudiant.getInscriptions());
    }
}
