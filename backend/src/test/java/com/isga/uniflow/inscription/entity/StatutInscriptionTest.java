package com.isga.uniflow.inscription.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste les règles du cycle de vie des statuts : transitions autorisées et statuts actifs.
 */
class StatutInscriptionTest {

    /** Matrice complète : les 9 combinaisons (départ, cible). */
    @ParameterizedTest(name = "{0} vers {1} : autorisé = {2}")
    @CsvSource({
            "EN_ATTENTE, VALIDEE,    true",
            "EN_ATTENTE, ANNULEE,    true",
            "EN_ATTENTE, EN_ATTENTE, false",
            "VALIDEE,    ANNULEE,    true",
            "VALIDEE,    EN_ATTENTE, false",
            "VALIDEE,    VALIDEE,    false",
            "ANNULEE,    EN_ATTENTE, false",
            "ANNULEE,    VALIDEE,    false",
            "ANNULEE,    ANNULEE,    false"
    })
    void peutPasserARespecteLeCycleDeVie(StatutInscription depart, StatutInscription cible, boolean attendu) {
        assertEquals(attendu, depart.peutPasserA(cible));
    }

    @Test
    void seulsEnAttenteEtValideeSontActifs() {
        assertTrue(StatutInscription.EN_ATTENTE.estActif());
        assertTrue(StatutInscription.VALIDEE.estActif());
        assertFalse(StatutInscription.ANNULEE.estActif());
    }

    @Test
    void nomsDesStatutsActifsNeContientPasLesInscriptionsAnnulees() {
        assertEquals(List.of("EN_ATTENTE", "VALIDEE"), StatutInscription.nomsDesStatutsActifs());
    }

    @Test
    void uneInscriptionAnnuleeNeRevientJamaisAUnStatutActif() {
        for (StatutInscription cible : StatutInscription.values()) {
            assertFalse(StatutInscription.ANNULEE.peutPasserA(cible), "ANNULEE vers " + cible);
        }
    }
}
