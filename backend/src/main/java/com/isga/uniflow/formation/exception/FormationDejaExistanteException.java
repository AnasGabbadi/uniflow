package com.isga.uniflow.formation.exception;

/**
 * Levée quand on crée une formation dont l'intitulé est déjà utilisé.
 * Renvoyée en HTTP 409 par {@link FormationExceptionHandler}.
 */
public class FormationDejaExistanteException extends RuntimeException {

    public FormationDejaExistanteException(String intitule) {
        super("Une formation avec l'intitulé '" + intitule + "' existe déjà");
    }
}