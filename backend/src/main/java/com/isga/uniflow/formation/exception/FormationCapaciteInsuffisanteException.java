package com.isga.uniflow.formation.exception;

/**
 * Levée quand on essaie de réduire la capacité d'une formation en dessous du nombre
 * d'inscriptions actives (EN_ATTENTE et VALIDEE) : ces étudiants ne pourraient plus avoir de place.
 * Renvoyée en HTTP 409 par {@link FormationExceptionHandler}.
 */
public class FormationCapaciteInsuffisanteException extends RuntimeException {

    public FormationCapaciteInsuffisanteException(int capaciteDemandee, long inscriptionsActives) {
        super("La capacité (" + capaciteDemandee
                + ") ne peut pas être inférieure au nombre d'inscriptions actives (" + inscriptionsActives + ")");
    }
}