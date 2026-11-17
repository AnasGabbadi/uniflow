package com.isga.uniflow.erreur;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Point unique de gestion des erreurs de l'API (IP-22).
 * Toutes les réponses d'erreur suivent le format défini dans docs/api-contract.md (§6).
 */
@RestControllerAdvice
public class GestionnaireErreurs extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GestionnaireErreurs.class);

    /** Contraintes d'unicité du schéma V1 et code d'erreur correspondant. */
    private static final Map<String, CodeErreur> CONTRAINTES_UNIQUES = Map.of(
            "uq_etudiant_email", CodeErreur.EMAIL_DEJA_UTILISE,
            "uq_formation_intitule", CodeErreur.INTITULE_DEJA_UTILISE,
            "uq_inscription_etudiant_formation", CodeErreur.INSCRIPTION_EN_DOUBLE);

    // ---------- 400 : validation des corps de requête (@Valid) ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErreurChamp> erreurs = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErreurChamp(e.getField(),
                        Objects.requireNonNullElse(e.getDefaultMessage(), "valeur invalide")))
                .sorted(Comparator.comparing(ErreurChamp::champ).thenComparing(ErreurChamp::message))
                .toList();

        ProblemeApi probleme = ProblemeApi.creer(HttpStatus.BAD_REQUEST, "Données invalides",
                "Un ou plusieurs champs sont invalides.", CodeErreur.VALIDATION);
        probleme.setErreurs(erreurs);
        return handleExceptionInternal(ex, probleme, headers, HttpStatus.BAD_REQUEST, request);
    }

    // ---------- Erreurs standard de Spring MVC (JSON mal formé, méthode non autorisée...) ----------

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> reponse = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (reponse == null || !(reponse.getBody() instanceof ProblemDetail detail)) {
            return reponse;
        }
        ProblemeApi probleme = detail instanceof ProblemeApi api
                ? api
                : new ProblemeApi(detail, codePourStatut(statusCode));
        if (probleme.getInstance() == null) {
            probleme.setInstance(URI.create(chemin(request)));
        }
        return ResponseEntity.status(reponse.getStatusCode()).headers(reponse.getHeaders()).body(probleme);
    }

    // ---------- 404 : ressource métier inconnue ----------

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ProblemeApi> gererRessourceIntrouvable(
            RessourceIntrouvableException ex, HttpServletRequest requete) {
        return reponse(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage(),
                CodeErreur.RESSOURCE_INTROUVABLE, requete);
    }

    // ---------- 409 : règle métier non respectée ----------

    @ExceptionHandler(ConflitMetierException.class)
    public ResponseEntity<ProblemeApi> gererConflitMetier(ConflitMetierException ex, HttpServletRequest requete) {
        return reponse(HttpStatus.CONFLICT, "Conflit", ex.getMessage(), ex.getCode(), requete);
    }

    // ---------- 409 / 400 : contrainte de base de données (filet de sécurité) ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemeApi> gererIntegrite(DataIntegrityViolationException ex, HttpServletRequest requete) {
        String cause = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
        return CONTRAINTES_UNIQUES.entrySet().stream()
                .filter(contrainte -> cause.contains(contrainte.getKey()))
                .map(contrainte -> reponse(HttpStatus.CONFLICT, "Conflit",
                        "Cette valeur est déjà utilisée.", contrainte.getValue(), requete))
                .findFirst()
                .orElseGet(() -> reponse(HttpStatus.BAD_REQUEST, "Requête invalide",
                        "Les données ne respectent pas les contraintes de la base.",
                        CodeErreur.REQUETE_INVALIDE, requete));
    }

    // ---------- 500 : erreur inattendue, sans détail technique ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemeApi> gererErreurInattendue(Exception ex, HttpServletRequest requete) {
        LOG.error("Erreur inattendue sur {} {}", requete.getMethod(), requete.getRequestURI(), ex);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue est survenue.", CodeErreur.ERREUR_INTERNE, requete);
    }

    // ---------- Outils ----------

    static CodeErreur codePourStatut(HttpStatusCode statut) {
        if (statut.value() == HttpStatus.NOT_FOUND.value()) {
            return CodeErreur.RESSOURCE_INTROUVABLE;
        }
        if (statut.is4xxClientError()) {
            return CodeErreur.REQUETE_INVALIDE;
        }
        return CodeErreur.ERREUR_INTERNE;
    }

    private static String chemin(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            return servlet.getRequest().getRequestURI();
        }
        return request.getDescription(false).replaceFirst("^uri=", "");
    }

    private static ResponseEntity<ProblemeApi> reponse(
            HttpStatus statut, String titre, String detail, CodeErreur code, HttpServletRequest requete) {
        ProblemeApi probleme = ProblemeApi.creer(statut, titre, detail, code);
        probleme.setInstance(URI.create(requete.getRequestURI()));
        return ResponseEntity.status(statut).body(probleme);
    }
}
