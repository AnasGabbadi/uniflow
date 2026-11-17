package com.isga.uniflow.inscription.entity;

import com.isga.uniflow.etudiant.entity.Etudiant;
import com.isga.uniflow.formation.entity.Formation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InscriptionTest {

    @Test
    void constructor_ShouldInitializeDefaultValues() {
        Inscription inscription = new Inscription();

        assertNull(inscription.getId());
        assertEquals(LocalDate.now(), inscription.getDateInscription());
        assertEquals("EN_ATTENTE", inscription.getStatut());
    }

    @Test
    void gettersAndSetters_ShouldHandleAllFields() {
        Inscription inscription = new Inscription();

        Etudiant etudiant = new Etudiant();
        Formation formation = new Formation();

        LocalDate dateInscription = LocalDate.of(2026, 11, 17);

        inscription.setEtudiant(etudiant);
        inscription.setFormation(formation);
        inscription.setDateInscription(dateInscription);
        inscription.setStatut("VALIDEE");

        assertSame(etudiant, inscription.getEtudiant());
        assertSame(formation, inscription.getFormation());
        assertEquals(dateInscription, inscription.getDateInscription());
        assertEquals("VALIDEE", inscription.getStatut());
    }

    @Test
    void setters_ShouldAllowUpdatingInscriptionFields() {
        Inscription inscription = new Inscription();

        inscription.setStatut("ANNULEE");
        inscription.setDateInscription(LocalDate.of(2026, 12, 1));

        assertEquals("ANNULEE", inscription.getStatut());
        assertEquals(
                LocalDate.of(2026, 12, 1),
                inscription.getDateInscription()
        );
    }
}
