package com.isga.uniflow.formation.exception;

import com.isga.uniflow.formation.controller.FormationController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestion des erreurs propre au module formation.
 *
 * Limité à FormationController : il n'affecte aucun autre contrôleur de l'équipe.
 * @Order garantit que ce handler passe avant un éventuel GlobalExceptionHandler.
 */
@RestControllerAdvice(assignableTypes = FormationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FormationExceptionHandler {

    /** Validation échouée (champ manquant, valeur négative, dates incohérentes) : 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erreurs = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erreurs.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> erreurs.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));

        return build(HttpStatus.BAD_REQUEST, "Données invalides", erreurs);
    }

    /** JSON illisible ou date au mauvais format (attendu : yyyy-MM-dd) : 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Corps de requête illisible : vérifiez le JSON et le format des dates (yyyy-MM-dd)", null);
    }

    /** Identifiant dans l'URL qui n'est pas un nombre (ex. /api/formations/abc) : 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Identifiant invalide : '" + ex.getValue() + "'", null);
    }

    /** Intitulé déjà utilisé : 409. */
    @ExceptionHandler(FormationDejaExistanteException.class)
    public ResponseEntity<Map<String, Object>> handleDejaExistante(FormationDejaExistanteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** Capacité inférieure au nombre d'inscriptions actives : 409. */
    @ExceptionHandler(FormationCapaciteInsuffisanteException.class)
    public ResponseEntity<Map<String, Object>> handleCapaciteInsuffisante(FormationCapaciteInsuffisanteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** Aucune formation pour l'identifiant demandé : 404. */
    @ExceptionHandler(FormationIntrouvableException.class)
    public ResponseEntity<Map<String, Object>> handleIntrouvable(FormationIntrouvableException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message,
                                                      Map<String, String> erreurs) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("message", message);
        if (erreurs != null) {
            body.put("erreurs", erreurs);
        }
        return ResponseEntity.status(status).body(body);
    }
}