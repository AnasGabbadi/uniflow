package com.isga.uniflow.formation.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste les règles de validation de FormationRequest, sans démarrer Spring ni la base.
 */
class FormationRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private FormationRequest valide() {
        return new FormationRequest("Java avancé", "Spring Boot", 40, 20,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20));
    }

    @Test
    void requeteValideNeProduitAucuneErreur() {
        assertTrue(validator.validate(valide()).isEmpty());
    }

    @Test
    void dateFinIdentiqueADateDebutEstAcceptee() {
        FormationRequest r = new FormationRequest("Atelier", null, 8, 10,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1));
        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void dateFinAvantDateDebutEstRefusee() {
        FormationRequest r = new FormationRequest("Java", null, 40, 20,
                LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 1));
        Set<ConstraintViolation<FormationRequest>> erreurs = validator.validate(r);
        assertEquals(1, erreurs.size());
        assertEquals("La date de fin ne peut pas précéder la date de début",
                erreurs.iterator().next().getMessage());
    }

    @Test
    void intituleVideEstRefuse() {
        FormationRequest r = new FormationRequest("  ", null, 40, 20,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20));
        assertEquals(1, validator.validate(r).size());
    }

    @Test
    void capaciteNulleOuNegativeEstRefusee() {
        for (int capacite : new int[]{0, -5}) {
            FormationRequest r = new FormationRequest("Java", null, 40, capacite,
                    LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20));
            assertEquals(1, validator.validate(r).size(), "capacité " + capacite);
        }
    }

    @Test
    void dureeNulleOuNegativeEstRefusee() {
        for (int duree : new int[]{0, -1}) {
            FormationRequest r = new FormationRequest("Java", null, duree, 20,
                    LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 20));
            assertEquals(1, validator.validate(r).size(), "durée " + duree);
        }
    }

    @Test
    void champsObligatoiresAbsentsSontSignales() {
        FormationRequest r = new FormationRequest(null, null, null, null, null, null);
        // intitulé, durée, capacité, date de début, date de fin
        assertEquals(5, validator.validate(r).size());
    }
}