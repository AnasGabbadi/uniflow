package com.isga.uniflow.erreur;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

class GestionnaireErreursTest {

    private final GestionnaireErreurs gestionnaire = new GestionnaireErreurs();
    private final MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/api/etudiants/42");

    @Test
    void ressourceIntrouvableRenvoie404AvecCode() {
        ResponseEntity<ProblemeApi> reponse = gestionnaire.gererRessourceIntrouvable(
                new RessourceIntrouvableException("étudiant", 42), requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemeApi probleme = reponse.getBody();
        assertThat(probleme).isNotNull();
        assertThat(probleme.getCode()).isEqualTo(CodeErreur.RESSOURCE_INTROUVABLE);
        assertThat(probleme.getTitle()).isEqualTo("Ressource introuvable");
        assertThat(probleme.getDetail()).isEqualTo("Aucun étudiant avec l'identifiant 42.");
        assertThat(probleme.getInstance()).isEqualTo(URI.create("/api/etudiants/42"));
    }

    @Test
    void conflitMetierRenvoie409AvecLeCodeDeLException() {
        ResponseEntity<ProblemeApi> reponse = gestionnaire.gererConflitMetier(
                new ConflitMetierException(CodeErreur.FORMATION_COMPLETE, "La formation est complète."), requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody()).isNotNull();
        assertThat(reponse.getBody().getCode()).isEqualTo(CodeErreur.FORMATION_COMPLETE);
        assertThat(reponse.getBody().getDetail()).isEqualTo("La formation est complète.");
    }

    @Test
    void emailEnDoubleEnBaseRenvoie409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("violation",
                new IllegalStateException("duplicate key value violates unique constraint \"uq_etudiant_email\""));

        ResponseEntity<ProblemeApi> reponse = gestionnaire.gererIntegrite(ex, requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody()).isNotNull();
        assertThat(reponse.getBody().getCode()).isEqualTo(CodeErreur.EMAIL_DEJA_UTILISE);
    }

    @Test
    void autreContrainteEnBaseRenvoie400() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("violation",
                new IllegalStateException("new row violates check constraint \"ck_formation_duree\""));

        ResponseEntity<ProblemeApi> reponse = gestionnaire.gererIntegrite(ex, requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).isNotNull();
        assertThat(reponse.getBody().getCode()).isEqualTo(CodeErreur.REQUETE_INVALIDE);
    }

    @Test
    void erreurInattendueRenvoie500SansDetailTechnique() {
        ResponseEntity<ProblemeApi> reponse = gestionnaire.gererErreurInattendue(
                new IllegalStateException("mot de passe = secret123"), requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(reponse.getBody()).isNotNull();
        assertThat(reponse.getBody().getCode()).isEqualTo(CodeErreur.ERREUR_INTERNE);
        assertThat(reponse.getBody().getDetail())
                .isEqualTo("Une erreur inattendue est survenue.")
                .doesNotContain("secret123");
    }

    @Test
    void validationRenvoie400AvecLeDetailDesChampsTries() throws NoSuchMethodException {
        BeanPropertyBindingResult resultat = new BeanPropertyBindingResult(new Object(), "requete");
        resultat.addError(new FieldError("requete", "nom", "ne doit pas être vide"));
        resultat.addError(new FieldError("requete", "email", "doit être une adresse e-mail valide"));
        Method methode = GestionnaireErreursTest.class.getDeclaredMethod("methodeFactice", String.class);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new MethodParameter(methode, 0), resultat);

        ResponseEntity<Object> reponse = gestionnaire.handleMethodArgumentNotValid(ex, new HttpHeaders(),
                HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest("POST", "/api/etudiants")));

        assertThat(reponse).isNotNull();
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).isInstanceOf(ProblemeApi.class);
        ProblemeApi probleme = (ProblemeApi) reponse.getBody();
        assertThat(probleme.getCode()).isEqualTo(CodeErreur.VALIDATION);
        assertThat(probleme.getInstance()).isEqualTo(URI.create("/api/etudiants"));
        assertThat(probleme.getErreurs()).containsExactly(
                new ErreurChamp("email", "doit être une adresse e-mail valide"),
                new ErreurChamp("nom", "ne doit pas être vide"));
    }

    @Test
    void erreurStandardDeSpringEstConvertieAuFormatDeLApi() throws Exception {
        ResponseEntity<Object> reponse = gestionnaire.handleException(
                new HttpRequestMethodNotSupportedException("PATCH"), new ServletWebRequest(requete));

        assertThat(reponse).isNotNull();
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(reponse.getBody()).isInstanceOf(ProblemeApi.class);
        ProblemeApi probleme = (ProblemeApi) reponse.getBody();
        assertThat(probleme.getCode()).isEqualTo(CodeErreur.REQUETE_INVALIDE);
        assertThat(probleme.getInstance()).isEqualTo(URI.create("/api/etudiants/42"));
    }

    @Test
    void codeDeduitDuStatutHttp() {
        assertThat(GestionnaireErreurs.codePourStatut(HttpStatus.NOT_FOUND)).isEqualTo(CodeErreur.RESSOURCE_INTROUVABLE);
        assertThat(GestionnaireErreurs.codePourStatut(HttpStatus.UNSUPPORTED_MEDIA_TYPE))
                .isEqualTo(CodeErreur.REQUETE_INVALIDE);
        assertThat(GestionnaireErreurs.codePourStatut(HttpStatus.SERVICE_UNAVAILABLE))
                .isEqualTo(CodeErreur.ERREUR_INTERNE);
    }

    @SuppressWarnings("unused")
    private void methodeFactice(String parametre) {
        // Méthode utilisée uniquement pour construire un MethodParameter dans le test de validation.
    }
}