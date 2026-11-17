package com.isga.uniflow.formation.exception;

/**
 * Levée quand aucune formation ne correspond à l'identifiant demandé.
 * Renvoyée en HTTP 404 par {@link FormationExceptionHandler}.
 */
public class FormationIntrouvableException extends RuntimeException {

    public FormationIntrouvableException(Integer id) {
        super("Aucune formation avec l'identifiant " + id);
    }
}