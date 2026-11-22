package com.isga.uniflow.etudiant.dto;

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
 * Teste les règles de validation de EtudiantRequest, sans démarrer Spring ni la base.
 */
class EtudiantRequestValidationTest {

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

    private EtudiantRequest valide() {
        return new EtudiantRequest("El Amrani", "Sara", "sara.elamrani@example.ma",
                "+212 6 12 34 56 78", "12 rue des Écoles, Casablanca", LocalDate.of(2003, 4, 21));
    }

    private Set<ConstraintViolation<EtudiantRequest>> valider(EtudiantRequest request) {
        return validator.validate(request);
    }

    private boolean erreurSurChamp(Set<ConstraintViolation<EtudiantRequest>> violations, String champ) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(champ));
    }

    @Test
    void requeteCompleteEstValide() {
        assertTrue(valider(valide()).isEmpty());
    }

    @Test
    void seulsLesChampsObligatoiresSuffisent() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", "Sara", "sara@example.ma", null, null, null);

        assertTrue(valider(request).isEmpty());
    }

    @Test
    void nomVideEstRefuse() {
        EtudiantRequest request = new EtudiantRequest("  ", "Sara", "sara@example.ma", null, null, null);

        assertTrue(erreurSurChamp(valider(request), "nom"));
    }

    @Test
    void prenomAbsentEstRefuse() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", null, "sara@example.ma", null, null, null);

        assertTrue(erreurSurChamp(valider(request), "prenom"));
    }

    @Test
    void nomDePlusDe100CaracteresEstRefuse() {
        EtudiantRequest request = new EtudiantRequest("a".repeat(101), "Sara", "sara@example.ma", null, null, null);

        assertTrue(erreurSurChamp(valider(request), "nom"));
    }

    @Test
    void emailAuMauvaisFormatEstRefuse() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", "Sara", "pas-un-email", null, null, null);

        assertTrue(erreurSurChamp(valider(request), "email"));
    }

    @Test
    void emailAbsentEstRefuse() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", "Sara", null, null, null, null);

        assertTrue(erreurSurChamp(valider(request), "email"));
    }

    @Test
    void telephoneAvecDesLettresEstRefuse() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", "Sara", "sara@example.ma", "06-abc", null, null);

        assertTrue(erreurSurChamp(valider(request), "telephone"));
    }

    @Test
    void telephoneDePlusDe30CaracteresEstRefuse() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", "Sara", "sara@example.ma",
                "1".repeat(31), null, null);

        assertTrue(erreurSurChamp(valider(request), "telephone"));
    }

    @Test
    void adresseDePlusDe255CaracteresEstRefusee() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", "Sara", "sara@example.ma",
                null, "a".repeat(256), null);

        assertTrue(erreurSurChamp(valider(request), "adresse"));
    }

    @Test
    void dateDeNaissanceDansLeFuturEstRefusee() {
        EtudiantRequest request = new EtudiantRequest("El Amrani", "Sara", "sara@example.ma",
                null, null, LocalDate.now().plusDays(1));

        Set<ConstraintViolation<EtudiantRequest>> violations = valider(request);

        assertEquals(1, violations.size());
        assertTrue(erreurSurChamp(violations, "dateNaissance"));
    }

    @Test
    void prenomVideEstRefuse() {
        assertTrue(erreurSurChamp(valider(new EtudiantRequest("El Amrani", "", "sara@example.ma", null, null, null)), "prenom"));
    }

    @Test
    void prenomDePlusDe100CaracteresEstRefuse() {
        assertTrue(erreurSurChamp(valider(new EtudiantRequest("El Amrani", "a".repeat(101), "sara@example.ma", null, null, null)), "prenom"));
    }

    @Test
    void emailVideEstRefuse() {
        assertTrue(erreurSurChamp(valider(new EtudiantRequest("El Amrani", "Sara", "", null, null, null)), "email"));
    }

    @Test
    void emailDePlusDe255CaracteresEstRefuse() {
        String email = "a".repeat(251) + "@x.ma";   // 256 caractères
        assertTrue(erreurSurChamp(valider(new EtudiantRequest("El Amrani", "Sara", email, null, null, null)), "email"));
    }

    @Test
    void telephoneAvecChiffresEspacesPlusEtTiretEstAccepte() {
        assertTrue(valider(new EtudiantRequest("El Amrani", "Sara", "sara@example.ma", "+212 6-12 34 56 78", null, null)).isEmpty());
    }

    @Test
    void dateDeNaissanceAujourdhuiEstRefusee() {
        assertTrue(erreurSurChamp(valider(new EtudiantRequest("El Amrani", "Sara", "sara@example.ma", null, null, LocalDate.now())), "dateNaissance"));
    }
}
